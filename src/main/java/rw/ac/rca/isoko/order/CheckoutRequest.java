package rw.ac.rca.isoko.order;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record CheckoutRequest(@NotNull PaymentProvider paymentMethod, String stubMode,
 String deliveryMethod, @Size(max=255) String deliveryAddress) {
 public CheckoutRequest(PaymentProvider method,String stub) { this(method,stub,"HOME_KIGALI",null); }
}