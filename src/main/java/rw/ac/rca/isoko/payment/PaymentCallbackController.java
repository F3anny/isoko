package rw.ac.rca.isoko.payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import rw.ac.rca.isoko.common.exception.ApiException;
import rw.ac.rca.isoko.order.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Lab callback contract. Sign timestamp + newline + orderId + newline + status + newline + reference. */
@RestController @RequestMapping("/api/v1/payments")
public class PaymentCallbackController {
    private final OrderService orders;
    private final String secret;
    public PaymentCallbackController(OrderService orders,@Value("${isoko.payment.callback-secret}") String secret) {this.orders=orders;this.secret=secret;}
    public record Callback(@NotNull UUID orderId,@NotBlank @Pattern(regexp="SUCCESS|DECLINE|TIMEOUT") String status,@Size(max=64) String reference) {}
    @PostMapping("/callback") public OrderResponse callback(@Valid @RequestBody Callback c,
            @RequestHeader("X-Payment-Timestamp") String timestamp,@RequestHeader("X-Payment-Signature") String signature) {
        try {
            long time=Long.parseLong(timestamp);
            if(Math.abs(Instant.now().getEpochSecond()-time)>300) throw ApiException.unauthorized("Invalid payment callback.");
            String payload=timestamp+"\n"+c.orderId()+"\n"+c.status()+"\n"+(c.reference()==null?"":c.reference());
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            byte[] expected=mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            if(!MessageDigest.isEqual(expected,HexFormat.of().parseHex(signature))) throw ApiException.unauthorized("Invalid payment callback.");
        } catch(ApiException e) {throw e;} catch(Exception e) {throw ApiException.unauthorized("Invalid payment callback.");}
        return orders.callback(c.orderId(),c.status(),c.reference());
    }
}
