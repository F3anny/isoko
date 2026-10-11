package rw.ac.rca.isoko.wishlist;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.rca.isoko.cart.CartResponse;
import rw.ac.rca.isoko.security.AuthPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@Tag(name = "Wishlist", description = "UC-CS-09. Unauthenticated callers receive 401 (login redirect on the storefront).")
public class WishlistController {

    private final WishlistService wishlists;

    public WishlistController(WishlistService wishlists) {
        this.wishlists = wishlists;
    }

    @GetMapping
    public List<WishlistResponse> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return wishlists.list(principal.userId());
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save an in-stock published product")
    public List<WishlistResponse> add(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID productId) {
        return wishlists.add(principal.userId(), productId);
    }

    @DeleteMapping("/{productId}")
    public List<WishlistResponse> remove(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID productId) {
        return wishlists.remove(principal.userId(), productId);
    }

    @PostMapping("/{productId}/move-to-cart")
    public CartResponse move(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID productId) {
        return wishlists.moveToCart(principal.userId(), productId);
    }
}
