package rw.ac.rca.isoko.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.isoko.catalog.Product;
import rw.ac.rca.isoko.catalog.ProductRepository;
import rw.ac.rca.isoko.catalog.ProductStatus;
import rw.ac.rca.isoko.commerce.DiscountCode;
import rw.ac.rca.isoko.commerce.DiscountCodeRepository;
import rw.ac.rca.isoko.commerce.Pricing;
import rw.ac.rca.isoko.common.exception.ApiException;
import rw.ac.rca.isoko.identity.LoyaltyTier;
import rw.ac.rca.isoko.identity.User;
import rw.ac.rca.isoko.identity.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository carts;
    private final ProductRepository products;
    private final DiscountCodeRepository discountCodes;
    private final UserRepository users;

    public CartService(CartRepository carts, ProductRepository products,
                       DiscountCodeRepository discountCodes, UserRepository users) {
        this.carts = carts;
        this.products = products;
        this.discountCodes = discountCodes;
        this.users = users;
    }

    @Transactional
    public CartResponse addItem(UUID userId, String guestToken, AddItemRequest request) {
        Cart cart = loadOrCreate(userId, guestToken);
        Product product = published(request.productId());
        if (!product.isInStock()) {
            throw ApiException.conflict("Product is out of stock.");
        }
        int existing = cart.itemFor(product.getId()).map(CartItem::getQty).orElse(0);
        int desired = existing + request.qty();
        if (desired > product.getStockQty()) {
            throw ApiException.conflict("Requested quantity exceeds remaining stock of " + product.getStockQty() + ".");
        }
        cart.itemFor(product.getId()).ifPresentOrElse(
                item -> item.setQty(desired),
                () -> cart.addItem(new CartItem(product.getId(), request.qty(), product.getPriceRwf()))
        );
        carts.save(cart);
        return toResponse(cart, userId);
    }

    @Transactional
    public CartResponse updateQty(UUID userId, String guestToken, UUID productId, int qty) {
        Cart cart = requireCart(userId, guestToken);
        if (qty <= 0) {
            cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        } else {
            Product product = published(productId);
            if (qty > product.getStockQty()) {
                throw ApiException.conflict("Requested quantity exceeds remaining stock of " + product.getStockQty() + ".");
            }
            CartItem item = cart.itemFor(productId).orElseThrow(() -> ApiException.notFound("Item is not in the cart."));
            item.setQty(qty);
            item.setUnitPriceRwf(product.getPriceRwf());
        }
        carts.save(cart);
        return toResponse(cart, userId);
    }

    @Transactional
    public CartResponse applyDiscount(UUID userId, String guestToken, String rawCode) {
        Pricing.assertCodeFormat(rawCode);
        Cart cart = requireCart(userId, guestToken);
        DiscountCode code = discountCodes.findByCodeIgnoreCase(rawCode.trim())
                .orElseThrow(() -> ApiException.badRequest("code is not valid"));
        if (!code.isUsable(Instant.now(), cart.totalQuantity())) {
            throw ApiException.badRequest("code is not valid");
        }
        cart.setDiscountCode(code.getCode());
        carts.save(cart);
        return toResponse(cart, userId);
    }

    @Transactional
    public CartResponse clearDiscount(UUID userId, String guestToken) {
        Cart cart = requireCart(userId, guestToken);
        cart.setDiscountCode(null);
        carts.save(cart);
        return toResponse(cart, userId);
    }

    @Transactional
    public CartResponse get(UUID userId, String guestToken) {
        Cart cart = loadOrCreate(userId, guestToken);
        return toResponse(cart, userId);
    }

    @Transactional
    public void mergeGuestCart(UUID userId, String guestToken) {
        if (guestToken == null || guestToken.isBlank()) {
            return;
        }
        Optional<Cart> guest = carts.findByGuestToken(guestToken);
        if (guest.isEmpty()) {
            return;
        }
        Cart userCart = carts.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(userId);
            return c;
        });
        for (CartItem guestItem : new ArrayList<>(guest.get().getItems())) {
            userCart.itemFor(guestItem.getProductId()).ifPresentOrElse(
                    existing -> existing.setQty(existing.getQty() + guestItem.getQty()),
                    () -> userCart.addItem(new CartItem(guestItem.getProductId(), guestItem.getQty(), guestItem.getUnitPriceRwf()))
            );
        }
        if (userCart.getDiscountCode() == null) {
            userCart.setDiscountCode(guest.get().getDiscountCode());
        }
        carts.save(userCart);
        carts.delete(guest.get());
    }

    public Cart requireOwnedCart(UUID userId) {
        return carts.findByUserId(userId).orElseThrow(() -> ApiException.badRequest("Cart is empty."));
    }

    Cart loadOrCreate(UUID userId, String guestToken) {
        if (userId != null) {
            return carts.findByUserId(userId).orElseGet(() -> {
                Cart c = new Cart();
                c.setUserId(userId);
                return carts.save(c);
            });
        }
        if (guestToken != null && !guestToken.isBlank()) {
            return carts.findByGuestToken(guestToken).orElseGet(() -> newGuest(guestToken));
        }
        return newGuest(UUID.randomUUID().toString().replace("-", ""));
    }

    private Cart requireCart(UUID userId, String guestToken) {
        if (userId != null) {
            return carts.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Cart not found"));
        }
        if (guestToken == null) {
            throw ApiException.badRequest("Provide Authorization or X-Cart-Token.");
        }
        return carts.findByGuestToken(guestToken).orElseThrow(() -> ApiException.notFound("Cart not found"));
    }

    private Cart newGuest(String token) {
        Cart c = new Cart();
        c.setGuestToken(token);
        return carts.save(c);
    }

    private Product published(UUID id) {
        Product product = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
        if (!product.isPublished()) {
            throw ApiException.notFound("Product not found");
        }
        return product;
    }

    public CartResponse toResponse(Cart cart, UUID userId) {
        LoyaltyTier tier = LoyaltyTier.STANDARD;
        if (userId != null) {
            tier = users.findById(userId).map(User::getLoyaltyTier).orElse(LoyaltyTier.STANDARD);
        }
        Integer codePct = null;
        if (cart.getDiscountCode() != null) {
            codePct = discountCodes.findByCodeIgnoreCase(cart.getDiscountCode())
                    .filter(c -> c.isUsable(Instant.now(), cart.totalQuantity()))
                    .map(DiscountCode::getPercentOff)
                    .orElse(null);
        }
        long subtotal = cart.subtotalRwf();
        long discount = Pricing.discountRwf(subtotal, cart.totalQuantity(), codePct);
        long after = subtotal - discount;
        long shipping = Pricing.shippingRwf(subtotal, tier);
        var lines = cart.getItems().stream().map(item -> {
            Product p = products.findById(item.getProductId()).orElse(null);
            String sku = p == null ? "" : p.getSku();
            String name = p == null ? "" : p.getName();
            return new CartResponse.CartLineResponse(
                    item.getProductId(), sku, name, item.getQty(), item.getUnitPriceRwf(),
                    item.getQty() * item.getUnitPriceRwf(),
                    p == null ? null : p.getImageUrl());
        }).toList();
        return new CartResponse(
                cart.getId(),
                cart.getGuestToken(),
                lines,
                cart.getDiscountCode(),
                subtotal,
                discount,
                shipping,
                after + shipping,
                tier
        );
    }
}
