package rw.ac.rca.isoko.wishlist;

import java.time.Instant;
import java.util.UUID;

public record WishlistResponse(
        UUID productId,
        String sku,
        String name,
        long priceRwf,
        boolean available,
        Instant addedAt,
        String imageUrl
) {}
