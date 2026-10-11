package rw.ac.rca.isoko.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import rw.ac.rca.isoko.cart.*;
import rw.ac.rca.isoko.catalog.*;
import rw.ac.rca.isoko.common.exception.ApiException;
import rw.ac.rca.isoko.config.IsokoProperties;
import rw.ac.rca.isoko.identity.UserRepository;
import rw.ac.rca.isoko.commerce.Pricing;
import rw.ac.rca.isoko.payment.PaymentGateway;
import rw.ac.rca.isoko.marketplace.MarketplaceRecords;
import java.time.Instant;
import java.util.*;
import java.security.SecureRandom;

@Service
@Transactional
public class OrderService {
 private final OrderRepository orders;
 private final CartService cartService;
 private final CartRepository carts;
 private final ProductRepository products;
 private final UserRepository users;
 private final PaymentGateway payments;
 private final MarketplaceRecords records;
 private final PasswordEncoder encoder;
 private final SecureRandom random = new SecureRandom();
 public OrderService(OrderRepository orders,CartService cartService,CartRepository carts,ProductRepository products,
 UserRepository users,PaymentGateway payments,MarketplaceRecords records,PasswordEncoder encoder) {
 this.orders=orders;this.cartService=cartService;this.carts=carts;this.products=products;this.users=users;
 this.payments=payments;this.records=records;this.encoder=encoder;
 }
 public OrderResponse checkout(UUID userId,CheckoutRequest request,String key) {
 if(key==null || key.isBlank() || key.length()>80) throw ApiException.badRequest("Provide an Idempotency-Key of 1–80 characters.");
 records.lockCheckoutKey(key.trim());
 var existing=orders.findByIdempotencyKey(key.trim());
 if(existing.isPresent()) {
 if(!existing.get().getUserId().equals(userId)) throw ApiException.conflict("Idempotency key is already in use.");
 return OrderResponse.from(existing.get());
 }
 var user=users.findById(userId).orElseThrow(()->ApiException.unauthorized("Authentication is required."));
 Cart cart=cartService.requireOwnedCart(userId);
 if(cart.getItems().isEmpty()) throw ApiException.badRequest("Cart is empty.");
 String method=request.deliveryMethod()==null?"HOME_KIGALI":request.deliveryMethod();
 if(!Set.of("PICKUP","HOME_KIGALI","HOME_OTHER").contains(method)) throw ApiException.badRequest("Choose a valid delivery method.");
 if(request.deliveryAddress()==null || request.deliveryAddress().isBlank()) throw ApiException.badRequest("Enter a delivery address or pickup point.");
 // Always lock stock in a deterministic order to prevent overselling and deadlocks.
 var sorted=cart.getItems().stream().sorted(Comparator.comparing(CartItem::getProductId)).toList();
 for(CartItem item:sorted) {
 Product p=products.lockById(item.getProductId()).orElseThrow(()->ApiException.conflict("A cart item is unavailable."));
 if(!p.isPublished() || item.getQty()>p.getStockQty()) throw ApiException.conflict("Item "+p.getSku()+" is unavailable or out of stock.");
 item.setUnitPriceRwf(p.getPriceRwf());
 }
 CartResponse priced=cartService.toResponse(cart,userId);
 ShopOrder o=new ShopOrder();o.setId(UUID.randomUUID());o.setUserId(userId);
 o.setOrderNumber("ISK-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT));
 o.setIdempotencyKey(key.trim());o.setPlacedAt(Instant.now());o.setUpdatedAt(Instant.now());
 o.setSubtotalRwf(priced.subtotalRwf());o.setDiscountRwf(priced.discountRwf());
 o.setShippingRwf(Pricing.shippingRwf(priced.subtotalRwf(),user.getLoyaltyTier(),method));
 o.setTotalRwf(o.getSubtotalRwf()-o.getDiscountRwf()+o.getShippingRwf());
 var feeSettings=records.settings();o.setCommissionPercent(((Number)feeSettings.get("commission_percent")).intValue());o.setPaymentFeePercent(((Number)feeSettings.get("payment_fee_percent")).intValue());
 o.setDeliveryMethod(method);o.setDeliveryAddress(request.deliveryAddress().trim());
 for(CartItem item:sorted) {
 Product p=products.findById(item.getProductId()).orElseThrow();
 OrderLine l=new OrderLine();l.setProductId(p.getId());l.setSku(p.getSku());l.setName(p.getName());
 l.setQty(item.getQty());l.setUnitPriceRwf(p.getPriceRwf());
 l.setShopId(p.getShop()==null?null:p.getShop().getId());l.setShopName(p.getShop()==null?"Isoko":p.getShop().getName());o.addLine(l);
 p.setStockQty(p.getStockQty()-item.getQty());
 }
 o.setStockReserved(true);o=orders.saveAndFlush(o);
 charge(o,request);
 // Keep declined/timed-out baskets so buyers can return to their cart.
 if(o.getStatus()==OrderStatus.PAID) { cart.getItems().clear();cart.setDiscountCode(null);carts.save(cart); }
 orders.saveAndFlush(o);return OrderResponse.from(o);
 }
 private void charge(ShopOrder o,CheckoutRequest request) {
 OrderStateMachine.assertTransition(o.getStatus(),OrderStatus.PAYMENT_PENDING);o.setStatus(OrderStatus.PAYMENT_PENDING);
 var result=payments.charge(request.paymentMethod(),o.getTotalRwf(),o.getId(),parseStub(request.stubMode()));
 Payment p=o.getPayment();if(p==null){p=new Payment();p.setOrder(o);o.setPayment(p);}
 p.setProvider(request.paymentMethod());p.setAmountRwf(o.getTotalRwf());p.setStatus(result.status());p.setGatewayRef(result.gatewayRef());
 if(result.status()==PaymentStatus.SUCCEEDED) {
 o.setStatus(OrderStatus.PAID);o.setEscrowState("HELD");
 records.move(o.getId(),"capture:"+o.getId(),"gateway","escrow:"+o.getId(),o.getTotalRwf());
 records.notify(o.getUserId(),o.getOrderNumber()+" paid. Your payment is held in escrow until delivery and the dispute window close.");
 } else if(result.status()==PaymentStatus.TIMED_OUT) o.setStatus(OrderStatus.PAYMENT_TIMED_OUT);
 else {o.setStatus(OrderStatus.PLACED);releaseStock(o);}
 records.audit(o.getUserId(),"PAYMENT_"+result.status(),o.getId(),request.paymentMethod().name());
 }
 @Transactional(readOnly=true)
 public OrderResponse getForUser(UUID user,UUID id) {return OrderResponse.from(owned(user,id,false));}
 @Transactional(readOnly=true)
 public List<OrderResponse> listForUser(UUID user) {return orders.findByUserIdOrderByPlacedAtDesc(user).stream().map(OrderResponse::from).toList();}
 public OrderResponse retryPayment(UUID user,UUID id,CheckoutRequest request) {
 ShopOrder o=owned(user,id,true);
 if(o.getStatus()!=OrderStatus.PLACED && o.getStatus()!=OrderStatus.PAYMENT_TIMED_OUT) throw ApiException.conflict("Order is not awaiting payment.");
 if(o.getPlacedAt().plusSeconds(600).isBefore(Instant.now())) throw ApiException.conflict("Order expired. Return to cart and try again.");
 if(!o.getEscrowState().equals("NONE")) throw ApiException.conflict("Payment is being reconciled. Do not submit another charge.");
 if(!o.isStockReserved()) reserve(o);
 charge(o,request);return OrderResponse.from(o);
 }
 private void reserve(ShopOrder o) {
 for(OrderLine l:o.getLines().stream().sorted(Comparator.comparing(OrderLine::getProductId)).toList()) {
 Product p=products.lockById(l.getProductId()).orElseThrow();
 if(!p.isPublished() || p.getStockQty()<l.getQty()) throw ApiException.conflict("Item is no longer available.");
 p.setStockQty(p.getStockQty()-l.getQty());
 }o.setStockReserved(true);
 }
 public OrderResponse cancel(UUID user,UUID id) {
 ShopOrder o=owned(user,id,true);cancelOrder(o,user);return OrderResponse.from(o);
 }
 private void cancelOrder(ShopOrder o,UUID actor) {
 if(o.getLines().stream().anyMatch(l->l.getShippedAt()!=null)) throw ApiException.conflict("An item has already shipped.");
 OrderStateMachine.assertTransition(o.getStatus(),OrderStatus.CANCELLED);boolean paid=o.getStatus()==OrderStatus.PAID || o.getEscrowState().equals("HELD") || o.getEscrowState().equals("FROZEN");
 o.setStatus(OrderStatus.CANCELLED);releaseStock(o);
 if(paid) refund(o);
 records.audit(actor,"ORDER_CANCELLED",o.getId(),o.getStatus().name());
 records.notify(o.getUserId(),o.getOrderNumber()+" cancelled. Return to cart and try again.");
 }
 private void releaseStock(ShopOrder o) {
 if(!o.isStockReserved()) return;
 for(OrderLine l:o.getLines().stream().sorted(Comparator.comparing(OrderLine::getProductId)).toList()) {
 Product p=products.lockById(l.getProductId()).orElseThrow();p.setStockQty(p.getStockQty()+l.getQty());
 }o.setStockReserved(false);
 }
 private void refund(ShopOrder o) {
 OrderStateMachine.assertTransition(o.getStatus(),OrderStatus.REFUNDED);
 records.move(o.getId(),"refund:"+o.getId(),"escrow:"+o.getId(),"gateway",o.getTotalRwf());
 o.setStatus(OrderStatus.REFUNDED);o.setEscrowState("REFUNDED");
 records.notify(o.getUserId(),o.getOrderNumber()+" refunded to "+o.getPayment().getProvider()+" (sandbox).");
 }
 public OrderResponse ship(UUID id,String tracking) {return shipShop(id,null,tracking,true);}
 public OrderResponse shipShop(UUID id,UUID shop,String tracking,boolean admin) {
 ShopOrder o=locked(id);
 if(o.getStatus()!=OrderStatus.PAID) throw ApiException.conflict("Only paid orders can be shipped.");
 if(tracking==null || tracking.isBlank() || tracking.length()>64) throw ApiException.badRequest("Tracking number is required (maximum 64 characters).");
 List<OrderLine> lines=o.getLines().stream().filter(l->admin || Objects.equals(l.getShopId(),shop)).toList();
 if(lines.isEmpty()) throw ApiException.notFound("Order not found");
 if(lines.stream().allMatch(l->l.getShippedAt()!=null)) throw ApiException.conflict("These items already shipped.");
 String otp=String.format("%06d",random.nextInt(1_000_000));String hash=encoder.encode(otp);
 for(OrderLine l:lines) if(l.getShippedAt()==null) {l.setTrackingNo(tracking.trim());l.setShippedAt(Instant.now());l.setDeliveryOtpHash(hash);}
 if(o.getLines().stream().allMatch(l->l.getShippedAt()!=null)) {OrderStateMachine.assertTransition(o.getStatus(),OrderStatus.SHIPPED);o.setStatus(OrderStatus.SHIPPED);}
 records.notify(o.getUserId(),o.getOrderNumber()+" shipment "+tracking.trim()+" dispatched. Delivery OTP: "+otp+". Share it only when receiving this parcel.");
 records.audit(null,"ORDER_SHIPPED",o.getId(),tracking.trim());return OrderResponse.from(o);
 }
 public OrderResponse confirmDelivery(UUID user,UUID id,UUID lineId,String otp) {
 ShopOrder o=owned(user,id,true);
 if(o.getStatus()!=OrderStatus.SHIPPED && o.getStatus()!=OrderStatus.PAID) throw ApiException.conflict("Order is not awaiting delivery.");
 OrderLine l=o.getLines().stream().filter(x->x.getId().equals(lineId)).findFirst().orElseThrow(()->ApiException.notFound("Item not found"));
 if(l.getShippedAt()==null || l.getDeliveredAt()!=null) throw ApiException.conflict("Item is not awaiting delivery.");
 if(otp==null || !encoder.matches(otp,l.getDeliveryOtpHash())) throw ApiException.badRequest("Invalid delivery OTP.");
 l.setDeliveredAt(Instant.now());l.setDeliveryOtpHash(null);finishDelivery(o);return OrderResponse.from(o);
 }
 public OrderResponse deliver(UUID id) {
 ShopOrder o=locked(id);OrderStateMachine.assertTransition(o.getStatus(),OrderStatus.DELIVERED);
 for(OrderLine l:o.getLines()) {l.setDeliveredAt(Instant.now());l.setDeliveryOtpHash(null);}finishDelivery(o);return OrderResponse.from(o);
 }
 private void finishDelivery(ShopOrder o) {
 if(o.getLines().stream().allMatch(l->l.getDeliveredAt()!=null)) {
 o.setStatus(OrderStatus.DELIVERED);o.setDeliveredAt(Instant.now());
 records.notify(o.getUserId(),o.getOrderNumber()+" delivered. You have 48 hours to open a dispute before seller payouts are released.");
 }
 }
 public OrderResponse dispute(UUID user,UUID id,String reason,String evidence) {
 ShopOrder o=owned(user,id,true);OrderStateMachine.assertTransition(o.getStatus(),OrderStatus.DISPUTED);
 if(o.getDeliveredAt()==null || o.getDeliveredAt().plusSeconds(172800).isBefore(Instant.now())) throw ApiException.conflict("The 48-hour dispute window has closed.");
 if(reason==null || reason.isBlank() || reason.length()>2000) throw ApiException.badRequest("Enter a dispute reason (maximum 2,000 characters).");
 if(evidence!=null && (evidence.length()>500 || !evidence.startsWith("https://"))) throw ApiException.badRequest("Evidence must be an HTTPS link of at most 500 characters.");
 o.setDisputeReason(reason.trim());o.setDisputeEvidence(evidence);o.setStatus(OrderStatus.DISPUTED);o.setEscrowState("FROZEN");
 records.audit(user,"DISPUTE_OPENED",id,reason);records.notify(user,"Dispute opened for "+o.getOrderNumber()+". Seller payout is frozen.");return OrderResponse.from(o);
 }
 public OrderResponse resolve(UUID actor,UUID id,boolean refund,String note) {
 ShopOrder o=locked(id);if(o.getStatus()!=OrderStatus.DISPUTED) throw ApiException.conflict("Order has no open dispute.");
 if(note==null || note.isBlank() || note.length()>2000) throw ApiException.badRequest("A resolution note is required.");
 o.setResolutionNote(note.trim());if(refund) refund(o);else complete(o);
 records.audit(actor,"DISPUTE_RESOLVED",id,(refund?"REFUND: ":"RELEASE: ")+note);return OrderResponse.from(o);
 }
 private void complete(ShopOrder o) {
 OrderStateMachine.assertTransition(o.getStatus(),OrderStatus.COMPLETED);
 long merchandise=o.getSubtotalRwf()-o.getDiscountRwf();
 Map<String,Long> totals=new TreeMap<>();
 for(OrderLine l:o.getLines()) totals.merge(l.getShopId()==null?"platform":l.getShopId().toString(),l.getQty()*l.getUnitPriceRwf(),Long::sum);
 long allocated=0;int index=0;
 for(var entry:totals.entrySet()) {
 long gross=++index==totals.size()?merchandise-allocated:merchandise*entry.getValue()/Math.max(1,o.getSubtotalRwf());allocated+=gross;
 long commission=gross*o.getCommissionPercent()/100;long paymentFee=gross*o.getPaymentFeePercent()/100;
 records.move(o.getId(),"payout:"+o.getId()+":"+entry.getKey(),"escrow:"+o.getId(),"seller:"+entry.getKey(),gross-commission-paymentFee);
 records.move(o.getId(),"commission:"+o.getId()+":"+entry.getKey(),"escrow:"+o.getId(),"commission",commission);
 records.move(o.getId(),"payment-fee:"+o.getId()+":"+entry.getKey(),"escrow:"+o.getId(),"payment-fees",paymentFee);
 }
 records.move(o.getId(),"shipping:"+o.getId(),"escrow:"+o.getId(),"shipping",o.getShippingRwf());
 o.setStatus(OrderStatus.COMPLETED);o.setEscrowState("RELEASED");o.setStockReserved(false);
 records.audit(null,"ESCROW_RELEASED",o.getId(),"Sandbox seller payouts; commission "+o.getCommissionPercent()+"%, payment fee "+o.getPaymentFeePercent()+"%");records.notify(o.getUserId(),o.getOrderNumber()+" completed. Seller payouts released (sandbox).");
 }
 public OrderResponse callback(UUID id,String status,String ref) {
 ShopOrder o=locked(id);
 if(status.equals("SUCCESS")) {
 if(o.getEscrowState().equals("RELEASED") || o.getEscrowState().equals("REFUNDED") || o.getStatus()==OrderStatus.PAID || o.getStatus()==OrderStatus.SHIPPED || o.getStatus()==OrderStatus.DELIVERED || o.getStatus()==OrderStatus.DISPUTED) return OrderResponse.from(o);
 if(o.getStatus()==OrderStatus.CANCELLED) {
 o.getPayment().setStatus(PaymentStatus.SUCCEEDED);o.getPayment().setGatewayRef(ref);
 records.move(id,"capture:"+id,"gateway","escrow:"+id,o.getTotalRwf());o.setEscrowState("HELD");refund(o);
 } else {
 if(o.getStatus()!=OrderStatus.PAYMENT_TIMED_OUT && o.getStatus()!=OrderStatus.PLACED && o.getStatus()!=OrderStatus.PAYMENT_PENDING) throw ApiException.conflict("Order cannot accept this payment callback.");
 if(!o.isStockReserved()) {
 o.getPayment().setStatus(PaymentStatus.SUCCEEDED);o.getPayment().setGatewayRef(ref);
 // A late success with no stock reservation must be returned to the original provider.
 records.move(id,"capture:"+id,"gateway","escrow:"+id,o.getTotalRwf());o.setStatus(OrderStatus.CANCELLED);refund(o);
 } else {
 o.setStatus(OrderStatus.PAID);o.setEscrowState("HELD");o.getPayment().setStatus(PaymentStatus.SUCCEEDED);o.getPayment().setGatewayRef(ref);
 records.move(id,"capture:"+id,"gateway","escrow:"+id,o.getTotalRwf());records.notify(o.getUserId(),o.getOrderNumber()+" payment reconciled successfully. Escrow is held.");
 }
 }
 } else if(status.equals("TIMEOUT")) {
 if(o.getStatus()==OrderStatus.PAID) {o.setStatus(OrderStatus.PAYMENT_TIMED_OUT);o.setEscrowState("FROZEN");records.notify(o.getUserId(),o.getOrderNumber()+" payment requires reconciliation. Do not pay again.");}
 else if(o.getStatus()==OrderStatus.PLACED || o.getStatus()==OrderStatus.PAYMENT_PENDING) o.setStatus(OrderStatus.PAYMENT_TIMED_OUT);
 } else if(status.equals("DECLINE") && o.getEscrowState().equals("NONE") && (o.getStatus()==OrderStatus.PAYMENT_PENDING || o.getStatus()==OrderStatus.PAYMENT_TIMED_OUT)) o.setStatus(OrderStatus.PLACED);
 records.audit(null,"PAYMENT_CALLBACK_"+status,id,"Signed sandbox callback");return OrderResponse.from(o);
 }

 public OrderResponse forceTransition(UUID id,OrderStatus to) {
 // Route admin actions through the same stock, escrow and delivery rules.
 ShopOrder o=locked(id);
 if(to==OrderStatus.CANCELLED) cancelOrder(o,null);
 else if(to==OrderStatus.DELIVERED) return deliver(id);
 else throw ApiException.conflict("Use the dedicated fulfilment or dispute action.");
 return OrderResponse.from(o);
 }
 public void processTimers() {
 for(var candidate:orders.findAll()) {
 ShopOrder o=locked(candidate.getId());Instant now=Instant.now();
 if((o.getStatus()==OrderStatus.PLACED || o.getStatus()==OrderStatus.PAYMENT_TIMED_OUT) && o.getEscrowState().equals("NONE") && o.getPlacedAt().plusSeconds(600).isBefore(now)) cancelOrder(o,null);
 else if(o.getStatus()==OrderStatus.DELIVERED && o.getDeliveredAt()!=null && o.getDeliveredAt().plusSeconds(172800).isBefore(now)) complete(o);
 }
 }
 private ShopOrder locked(UUID id) {return orders.lockById(id).orElseThrow(()->ApiException.notFound("Order not found"));}
 private ShopOrder owned(UUID user,UUID id,boolean lock) {
 ShopOrder o=lock?locked(id):orders.findById(id).orElseThrow(()->ApiException.notFound("Order not found"));
 if(!o.getUserId().equals(user)) throw ApiException.notFound("Order not found");return o;
 }
 private IsokoProperties.StubMode parseStub(String raw) {
 if(raw==null || raw.isBlank()) return null;
 try{return IsokoProperties.StubMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));}
 catch(IllegalArgumentException e){throw ApiException.badRequest("Unknown stubMode. Use SUCCESS, DECLINE or TIMEOUT.");}
 }
}
