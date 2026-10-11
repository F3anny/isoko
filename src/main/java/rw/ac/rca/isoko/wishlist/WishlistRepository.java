package rw.ac.rca.isoko.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<WishlistItem, WishlistItemId> {
    List<WishlistItem> findByIdUserIdOrderByAddedAtDesc(UUID userId);
    boolean existsByIdUserIdAndIdProductId(UUID userId, UUID productId);
    void deleteByIdUserIdAndIdProductId(UUID userId, UUID productId);
}
