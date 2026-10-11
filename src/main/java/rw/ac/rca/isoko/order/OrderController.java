package rw.ac.rca.isoko.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.rca.isoko.security.AuthPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Checkout & Orders", description = "UC-CS-07 Checkout · UC-CS-08 Pay · UC-CS-10 Track. Foreign orders return 404 (IDOR).")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place order and charge via the payment adapter stub")
    public OrderResponse checkout(@AuthenticationPrincipal AuthPrincipal principal,
                                  @Valid @RequestBody CheckoutRequest request,
                                  @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                  @RequestHeader(value = "X-Payment-Stub-Mode", required = false) String stubHeader) {
        CheckoutRequest effective = new CheckoutRequest(
                request.paymentMethod(),
                request.stubMode() != null ? request.stubMode() : stubHeader,
                request.deliveryMethod(), request.deliveryAddress()
        );
        return orders.checkout(principal.userId(), effective, idempotencyKey);
    }

    @GetMapping("/orders")
    public List<OrderResponse> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return orders.listForUser(principal.userId());
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get an order you own. Other customers' ids return 404, not 403.")
    public OrderResponse one(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        return orders.getForUser(principal.userId(), id);
    }

    @PostMapping("/orders/{id}/pay")
    @Operation(summary = "Retry payment for PLACED or PAYMENT_TIMED_OUT orders")
    public OrderResponse retry(@AuthenticationPrincipal AuthPrincipal principal,
                               @PathVariable UUID id,
                               @Valid @RequestBody CheckoutRequest request,
                               @RequestHeader(value = "X-Payment-Stub-Mode", required = false) String stubHeader) {
        CheckoutRequest effective = new CheckoutRequest(
                request.paymentMethod(),
                request.stubMode() != null ? request.stubMode() : stubHeader,
                request.deliveryMethod(), request.deliveryAddress()
        );
        return orders.retryPayment(principal.userId(), id, effective);
    }
}
