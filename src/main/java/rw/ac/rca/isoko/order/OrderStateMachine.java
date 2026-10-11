package rw.ac.rca.isoko.order;
import rw.ac.rca.isoko.common.exception.ApiException;
public final class OrderStateMachine {
 private OrderStateMachine() {}
 public static void assertTransition(OrderStatus from, OrderStatus to) {
 boolean legal = switch (from) {
 case PLACED -> to == OrderStatus.PAYMENT_PENDING || to == OrderStatus.PAID || to == OrderStatus.CANCELLED || to == OrderStatus.PAYMENT_TIMED_OUT;
 case PAYMENT_PENDING -> to == OrderStatus.PAID || to == OrderStatus.PLACED || to == OrderStatus.PAYMENT_TIMED_OUT || to == OrderStatus.CANCELLED;
 case PAYMENT_TIMED_OUT -> to == OrderStatus.PAYMENT_PENDING || to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
 case PAID -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
 case SHIPPED -> to == OrderStatus.DELIVERED;
 case DELIVERED -> to == OrderStatus.COMPLETED || to == OrderStatus.DISPUTED;
 case DISPUTED -> to == OrderStatus.REFUNDED || to == OrderStatus.COMPLETED;
 case CANCELLED -> to == OrderStatus.REFUNDED;
 case COMPLETED, REFUNDED -> false;
 };
 if (!legal) throw ApiException.conflict("Illegal order transition: " + from + " → " + to);
 }
}