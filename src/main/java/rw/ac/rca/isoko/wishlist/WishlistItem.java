package rw.ac.rca.isoko.wishlist;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wishlist_items", schema = "cloud_sphere")
public class WishlistItem {

    @EmbeddedId
    private WishlistItemId id;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    public WishlistItem(UUID userId, UUID productId) {
        this.id = new WishlistItemId(userId, productId);
    }

    @PrePersist
    void onCreate() {
        addedAt = Instant.now();
    }
}
