package rw.ac.rca.isoko.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "shipments", schema = "cloud_sphere")
public class Shipment {

    @Id
    private UUID id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @Column(name = "tracking_no", nullable = false)
    private String trackingNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentState state = ShipmentState.SHIPPED;

    @Column(name = "shipped_at", nullable = false)
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (shippedAt == null) {
            shippedAt = Instant.now();
        }
    }
}
