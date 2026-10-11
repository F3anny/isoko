package rw.ac.rca.isoko.wishlist;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.isoko.cart.AddItemRequest;
import rw.ac.rca.isoko.cart.CartResponse;
import rw.ac.rca.isoko.cart.CartService;
import rw.ac.rca.isoko.catalog.Product;
import rw.ac.rca.isoko.catalog.ProductRepository;
import rw.ac.rca.isoko.catalog.ProductStatus;
import rw.ac.rca.isoko.common.exception.ApiException;

import java.util.List;
import java.util.UUID;

@Service
public class WishlistService {

    private final WishlistRepository wishlists;
    private final ProductRepository products;
    private final CartService carts;

    public WishlistService(WishlistRepository wishlists, ProductRepository products, CartService carts) {
        this.wishlists = wishlists;
        this.products = products;
        this.carts = carts;
    }

    @Transactional
    public List<WishlistResponse> add(UUID userId, UUID productId) {
        Product product = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product not found"));
        if (!product.isPublished()) {
            throw ApiException.notFound("Product not found");
        }
        if (!product.isInStock()) {
            throw ApiException.conflict("Out-of-stock products cannot be added to the wishlist.");
        }
        if (!wishlists.existsByIdUserIdAndIdProductId(userId, productId)) {
            wishlists.save(new WishlistItem(userId, productId));
        }
        return list(userId);
    }

    @Transactional(readOnly = true)
    public List<WishlistResponse> list(UUID userId) {
        return wishlists.findByIdUserIdOrderByAddedAtDesc(userId).stream()
                .map(item -> {
                    Product p = products.findById(item.getId().getProductId()).orElse(null);
                    boolean available = p != null && p.isPublished() && p.isInStock();
                    return new WishlistResponse(
                            item.getId().getProductId(),
                            p == null ? null : p.getSku(),
                            p == null ? null : p.getName(),
                            p == null ? 0 : p.getPriceRwf(),
                            available,
                            item.getAddedAt(),
                            p == null ? null : p.getImageUrl()
                    );
                })
                .toList();
    }

    @Transactional
    public List<WishlistResponse> remove(UUID userId, UUID productId) {
        wishlists.deleteByIdUserIdAndIdProductId(userId, productId);
        return list(userId);
    }

    @Transactional
    public CartResponse moveToCart(UUID userId, UUID productId) {
        if (!wishlists.existsByIdUserIdAndIdProductId(userId, productId)) {
            throw ApiException.notFound("Wishlist item not found");
        }
        return carts.addItem(userId, null, new AddItemRequest(productId, 1));
    }
}
