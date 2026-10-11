package rw.ac.rca.isoko.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders", schema = "cloud_sphere")
public class ShopOrder {

    @Id
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(name = "subtotal_rwf", nullable = false)
    private long subtotalRwf;

    @Column(name = "discount_rwf", nullable = false)
    private long discountRwf;

    @Column(name = "shipping_rwf", nullable = false)
    private long shippingRwf;

    @Column(name = "total_rwf", nullable = false)
    private long totalRwf;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Shipment shipment;

    @Column(name = "escrow_state", nullable = false) private String escrowState = "NONE";
    @Column(name = "delivery_method", nullable = false) private String deliveryMethod = "HOME_KIGALI";
    @Column(name = "delivery_address") private String deliveryAddress;
    @Column(name = "delivered_at") private Instant deliveredAt;
    @Column(name = "dispute_reason") private String disputeReason;
    @Column(name = "dispute_evidence") private String disputeEvidence;
    @Column(name = "resolution_note") private String resolutionNote;
    @Column(name = "stock_reserved", nullable = false) private boolean stockReserved;
    @jakarta.persistence.Version private long version;
    @Column(name = "commission_percent", nullable = false) private int commissionPercent = 8;
    @Column(name = "payment_fee_percent", nullable = false) private int paymentFeePercent = 2;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        placedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addLine(OrderLine line) {
        line.setOrder(this);
        lines.add(line);
    }
}
