package rw.ac.rca.isoko.order;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record OrderResponse(UUID id,String orderNumber,OrderStatus status,long subtotalRwf,
 long discountRwf,long shippingRwf,long totalRwf,Instant placedAt,
 PaymentView payment,ShipmentView shipment,List<LineView> lines,
 String escrowState,String deliveryMethod,String deliveryAddress,Instant deliveredAt,
 String disputeReason,String disputeEvidence,String resolutionNote) {
 public record PaymentView(PaymentProvider provider,PaymentStatus status,String gatewayRef,long amountRwf) {}
 public record ShipmentView(String trackingNo,ShipmentState state) {}
 public record LineView(UUID id,UUID productId,String sku,String name,int qty,long unitPriceRwf,
 UUID shopId,String shopName,String trackingNo,Instant shippedAt,Instant deliveredAt) {}
 public static OrderResponse from(ShopOrder o) {
 PaymentView pay=o.getPayment()==null?null:new PaymentView(o.getPayment().getProvider(),o.getPayment().getStatus(),mask(o.getPayment().getGatewayRef()),o.getPayment().getAmountRwf());
 ShipmentView ship=o.getShipment()==null?null:new ShipmentView(o.getShipment().getTrackingNo(),o.getShipment().getState());
 return new OrderResponse(o.getId(),o.getOrderNumber(),o.getStatus(),o.getSubtotalRwf(),o.getDiscountRwf(),o.getShippingRwf(),o.getTotalRwf(),o.getPlacedAt(),pay,ship,o.getLines().stream().map(l->new LineView(l.getId(),l.getProductId(),l.getSku(),l.getName(),l.getQty(),l.getUnitPriceRwf(),l.getShopId(),l.getShopName(),l.getTrackingNo(),l.getShippedAt(),l.getDeliveredAt())).toList(),o.getEscrowState(),o.getDeliveryMethod(),o.getDeliveryAddress(),o.getDeliveredAt(),o.getDisputeReason(),o.getDisputeEvidence(),o.getResolutionNote());
 }
 private static String mask(String ref) { return ref==null?null:"••••"+ref.substring(Math.max(0,ref.length()-4)); }
}