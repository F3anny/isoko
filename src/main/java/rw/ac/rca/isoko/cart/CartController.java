package rw.ac.rca.isoko.cart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.rca.isoko.security.AuthPrincipal;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "UC-CS-05 Add to cart · UC-CS-06 Apply discount. Guests send X-Cart-Token.")
@SecurityRequirements
public class CartController {

    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    @GetMapping
    @Operation(summary = "Get or create the current cart")
    public CartResponse get(@AuthenticationPrincipal AuthPrincipal principal,
                            @RequestHeader(value = "X-Cart-Token", required = false) String token,
                            HttpServletResponse response) {
        CartResponse body = carts.get(userId(principal), token);
        exposeToken(response, body);
        return body;
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal AuthPrincipal principal,
                            @RequestHeader(value = "X-Cart-Token", required = false) String token,
                            @Valid @RequestBody AddItemRequest request,
                            HttpServletResponse response) {
        CartResponse body = carts.addItem(userId(principal), token, request);
        exposeToken(response, body);
        return body;
    }

    @PatchMapping("/items/{productId}")
    public CartResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                               @RequestHeader(value = "X-Cart-Token", required = false) String token,
                               @PathVariable UUID productId,
                               @Valid @RequestBody UpdateQtyRequest body) {
        return carts.updateQty(userId(principal), token, productId, body.qty());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(@AuthenticationPrincipal AuthPrincipal principal,
                               @RequestHeader(value = "X-Cart-Token", required = false) String token,
                               @PathVariable UUID productId) {
        return carts.updateQty(userId(principal), token, productId, 0);
    }

    @PostMapping("/discount")
    @Operation(summary = "Apply a 5–10 character alphanumeric discount code")
    public CartResponse discount(@AuthenticationPrincipal AuthPrincipal principal,
                                 @RequestHeader(value = "X-Cart-Token", required = false) String token,
                                 @Valid @RequestBody ApplyDiscountRequest request) {
        return carts.applyDiscount(userId(principal), token, request.code());
    }

    @DeleteMapping("/discount")
    public CartResponse clearDiscount(@AuthenticationPrincipal AuthPrincipal principal,
                                      @RequestHeader(value = "X-Cart-Token", required = false) String token) {
        return carts.clearDiscount(userId(principal), token);
    }

    private static UUID userId(AuthPrincipal principal) {
        return principal == null ? null : principal.userId();
    }

    private static void exposeToken(HttpServletResponse response, CartResponse body) {
        if (body.guestToken() != null) {
            response.setHeader("X-Cart-Token", body.guestToken());
        }
    }
}
