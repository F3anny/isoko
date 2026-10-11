package rw.ac.rca.isoko;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import rw.ac.rca.isoko.config.DataInitializer;
import rw.ac.rca.isoko.order.*;
import rw.ac.rca.isoko.identity.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class IsokoMarketplaceTests {
    @Container static final PostgreSQLContainer<?> DB=new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    @Container static final GenericContainer<?> REDIS=new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    @DynamicPropertySource static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",DB::getJdbcUrl);r.add("spring.datasource.username",DB::getUsername);r.add("spring.datasource.password",DB::getPassword);
        r.add("spring.data.redis.host",REDIS::getHost);r.add("spring.data.redis.port",()->REDIS.getMappedPort(6379));
    }
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired OrderService service;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final ObjectMapper json=new ObjectMapper();
    private JsonNode request(String method,String path,String token,String body,int expected) throws Exception {
        var req=switch(method){case "POST"->post(path);case "PATCH"->patch(path);default->get(path);};
        if(token!=null)req.header("Authorization","Bearer "+token);
        if(body!=null)req.contentType("application/json").content(body);
        var response=mvc.perform(req).andReturn().getResponse();
        assertEquals(expected,response.getStatus(),response.getContentAsString());
        return response.getContentAsString().isBlank()?json.createObjectNode():json.readTree(response.getContentAsString());
    }
    private String login(String email) throws Exception {
        var data=request("POST","/api/v1/auth/login",null,"{\"email\":\""+email+"\",\"password\":\""+DataInitializer.DEMO_PASSWORD+"\"}",200);
        if(data.has("challengeId")) data=request("POST","/api/v1/auth/verify",null,"{\"challengeId\":\""+data.get("challengeId").asText()+"\",\"code\":\""+data.get("labCode").asText()+"\"}",200);
        return data.get("accessToken").asText();
    }
    private JsonNode place(String token,String product,String mode,String key) throws Exception {
        request("POST","/api/v1/cart/items",token,"{\"productId\":\""+product+"\",\"qty\":1}",200);
        var response=mvc.perform(post("/api/v1/checkout").header("Authorization","Bearer "+token).header("Idempotency-Key",key).contentType("application/json")
                .content("{\"paymentMethod\":\"AIRTEL_MONEY\",\"stubMode\":\""+mode+"\",\"deliveryMethod\":\"PICKUP\",\"deliveryAddress\":\"Kigali pickup\"}")).andReturn().getResponse();
        assertEquals(201,response.getStatus(),response.getContentAsString());return json.readTree(response.getContentAsString());
    }
    private void clearCart(String token) throws Exception {
        for(var item:request("GET","/api/v1/cart",token,null,200).get("items")) request("PATCH","/api/v1/cart/items/"+item.get("productId").asText(),token,"{\"qty\":0}",200);
    }
    private JsonNode callback(String id,String status,boolean valid) throws Exception {
        String timestamp=String.valueOf(java.time.Instant.now().getEpochSecond()),reference="lab-test-ref";
        String canonical=timestamp+"\n"+id+"\n"+status+"\n"+reference;
        var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec("isoko-lab-callback-secret-change-before-sharing".getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256"));
        String signature=valid?java.util.HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8))):"00";
        var response=mvc.perform(post("/api/v1/payments/callback").header("X-Payment-Timestamp",timestamp).header("X-Payment-Signature",signature).contentType("application/json")
                .content("{\"orderId\":\""+id+"\",\"status\":\""+status+"\",\"reference\":\""+reference+"\"}")).andReturn().getResponse();
        assertEquals(valid?200:401,response.getStatus(),response.getContentAsString());return json.readTree(response.getContentAsString());
    }
    @Test void escrowDeliveryDisputeRefundAndOwnership() throws Exception {
        String buyer=login("demo@isoko.rw"),seller=login("eric.seller@isoko.rw"),admin=login("ops@isoko.rw");
        var product=request("GET","/api/v1/products?q=TS-001",null,null,200).get("content").get(0);
        String key=UUID.randomUUID().toString();var order=place(buyer,product.get("id").asText(),"SUCCESS",key);String id=order.get("id").asText();
        assertEquals("PAID",order.get("status").asText());assertEquals("HELD",order.get("escrowState").asText());assertEquals(1000,order.get("shippingRwf").asLong());
        var repeated=mvc.perform(post("/api/v1/checkout").header("Authorization","Bearer "+buyer).header("Idempotency-Key",key).contentType("application/json").content("{\"paymentMethod\":\"AIRTEL_MONEY\"}")).andReturn().getResponse();
        assertEquals(id,json.readTree(repeated.getContentAsString()).get("id").asText());
        request("GET","/api/v1/orders/"+id,login("gold@isoko.rw"),null,404);
        request("POST","/api/v1/seller/orders/"+id+"/ship",seller,"{\"trackingNo\":\"ISK-TEST\"}",200);
        var inbox=request("GET","/api/v1/notifications",buyer,null,200);String otp=null;
        for(var n:inbox) if(n.get("message").asText().contains("ISK-TEST dispatched")) otp=n.get("message").asText().split("Delivery OTP: ")[1].substring(0,6);
        assertNotNull(otp);
        String line=order.get("lines").get(0).get("id").asText();
        String payload="{\"lineId\":\""+line+"\",\"otp\":\""+otp+"\"}";
        var delivered=request("POST","/api/v1/orders/"+id+"/confirm-delivery",buyer,payload,200);assertEquals("DELIVERED",delivered.get("status").asText());
        request("POST","/api/v1/orders/"+id+"/confirm-delivery",buyer,payload,409);
        var disputed=request("POST","/api/v1/orders/"+id+"/dispute",buyer,"{\"reason\":\"Wrong size\"}",200);assertEquals("FROZEN",disputed.get("escrowState").asText());
        var refunded=request("POST","/api/v1/admin/orders/"+id+"/resolve",admin,"{\"refund\":true,\"note\":\"Verified incorrect item\"}",200);assertEquals("REFUNDED",refunded.get("status").asText());
        request("POST","/api/v1/admin/orders/"+id+"/resolve",admin,"{\"refund\":true,\"note\":\"Duplicate\"}",409);
    }
    @Test void pendingShopHiddenAndSellerCannotEditPlatformProducts() throws Exception {
        assertEquals(0,request("GET","/api/v1/products?q=BK-010",null,null,200).get("totalElements").asInt());
        String seller=login("eric.seller@isoko.rw");
        String product=request("GET","/api/v1/products?q=USB-C-01",null,null,200).get("content").get(0).get("id").asText();
        request("PATCH","/api/v1/seller/products/"+product,seller,"{\"sku\":\"USB-C-01\",\"name\":\"Changed\",\"priceRwf\":1000,\"stockQty\":1,\"status\":\"PUBLISHED\"}",404);
        request("GET","/api/v1/admin/ledger",seller,null,403);
    }
    @Test void phoneVerificationIsSingleUseAndFailedLoginsPersist() throws Exception {
        String email="verify-"+UUID.randomUUID()+"@example.test";String phone="079"+String.format("%07d",new java.security.SecureRandom().nextInt(10000000));
        var challenge=request("POST","/api/v1/auth/register",null,"{\"email\":\""+email+"\",\"password\":\"StrongPass1\",\"phone\":\""+phone+"\"}",201);
        assertFalse(challenge.has("accessToken"));assertFalse(users.existsByEmailIgnoreCase(email));
        String verify="{\"challengeId\":\""+challenge.get("challengeId").asText()+"\",\"code\":\""+challenge.get("labCode").asText()+"\"}";
        request("POST","/api/v1/auth/verify",null,verify,200);request("POST","/api/v1/auth/verify",null,verify,400);
        for(int i=0;i<3;i++)request("POST","/api/v1/auth/login",null,"{\"email\":\""+email+"\",\"password\":\"WrongPass1\"}",401);
        var user=users.findByEmailIgnoreCase(email).orElseThrow();assertEquals(3,user.getFailedAttempts());assertEquals(UserStatus.LOCKED,user.getStatus());
        request("POST","/api/v1/auth/login",null,"{\"email\":\""+email+"\",\"password\":\"StrongPass1\"}",401);
    }
    @Test void deliveredOrderCompletesWithBalancedNetSellerPayout() throws Exception {
        String buyer=login("gold@isoko.rw"),admin=login("ops@isoko.rw");
        String product=request("GET","/api/v1/products?q=PH-500",null,null,200).get("content").get(0).get("id").asText();
        var order=place(buyer,product,"SUCCESS",UUID.randomUUID().toString());String id=order.get("id").asText();
        request("POST","/api/v1/admin/orders/"+id+"/ship",admin,"{\"trackingNo\":\"ISK-PAYOUT\"}",200);
        request("POST","/api/v1/admin/orders/"+id+"/deliver",admin,null,200);
        jdbc.update("UPDATE cloud_sphere.orders SET delivered_at=now()-interval '49 hours' WHERE id=?",UUID.fromString(id));
        service.processTimers();
        var completed=request("GET","/api/v1/orders/"+id,buyer,null,200);assertEquals("COMPLETED",completed.get("status").asText());
        Long payout=jdbc.queryForObject("SELECT sum(amount_rwf) FROM cloud_sphere.ledger_movements WHERE order_id=? AND credit_account LIKE 'seller:%'",Long.class,UUID.fromString(id));
        assertEquals(4500,payout);
        Long release=jdbc.queryForObject("SELECT sum(amount_rwf) FROM cloud_sphere.ledger_movements WHERE order_id=? AND debit_account LIKE 'escrow:%'",Long.class,UUID.fromString(id));
        assertEquals(order.get("totalRwf").asLong(),release);
        service.processTimers();
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM cloud_sphere.ledger_movements WHERE movement_key LIKE ?",Integer.class,"payout:"+id+":%"));
    }
    @Test void signedCallbacksAreReplaySafeAndTimeoutCannotTriggerAnotherCharge() throws Exception {
        String buyer=login("demo@isoko.rw");clearCart(buyer);
        String product=request("GET","/api/v1/products?q=USB-C-01",null,null,200).get("content").get(0).get("id").asText();
        var pending=place(buyer,product,"TIMEOUT",UUID.randomUUID().toString());String id=pending.get("id").asText();
        assertEquals("PAYMENT_TIMED_OUT",pending.get("status").asText());callback(id,"SUCCESS",false);
        assertEquals("PAID",callback(id,"SUCCESS",true).get("status").asText());callback(id,"SUCCESS",true);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM cloud_sphere.ledger_movements WHERE movement_key=?",Integer.class,"capture:"+id));
        assertEquals("FROZEN",callback(id,"TIMEOUT",true).get("escrowState").asText());
        request("POST","/api/v1/orders/"+id+"/pay",buyer,"{\"paymentMethod\":\"MTN_MOMO\"}",409);
        callback(id,"SUCCESS",true);request("POST","/api/v1/orders/"+id+"/cancel",buyer,null,200);
        assertEquals("REFUNDED",request("GET","/api/v1/orders/"+id,buyer,null,200).get("status").asText());clearCart(buyer);
    }
    @Test void concurrentBuyersCannotOversellLastUnit() throws Exception {
        String a=login("demo@isoko.rw"),b=login("gold@isoko.rw");clearCart(a);clearCart(b);
        String product=request("GET","/api/v1/products?q=LIM-001",null,null,200).get("content").get(0).get("id").asText();
        for(String token:List.of(a,b))request("POST","/api/v1/cart/items",token,"{\"productId\":\""+product+"\",\"qty\":1}",200);
        var barrier=new java.util.concurrent.CyclicBarrier(2);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<Integer>> results=new ArrayList<>();
            for(String token:List.of(a,b))results.add(pool.submit(()->{
                barrier.await();return mvc.perform(post("/api/v1/checkout").header("Authorization","Bearer "+token).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json")
                        .content("{\"paymentMethod\":\"MTN_MOMO\",\"deliveryMethod\":\"PICKUP\",\"deliveryAddress\":\"Test pickup\"}")).andReturn().getResponse().getStatus();
            }));
            var statuses=List.of(results.get(0).get(30,java.util.concurrent.TimeUnit.SECONDS),results.get(1).get(30,java.util.concurrent.TimeUnit.SECONDS));
            assertTrue(statuses.contains(201)&&statuses.contains(409),statuses.toString());
            assertEquals(0,jdbc.queryForObject("SELECT stock_qty FROM cloud_sphere.products WHERE id=?",Integer.class,UUID.fromString(product)));
        } finally {pool.shutdownNow();clearCart(a);clearCart(b);}
    }
}
