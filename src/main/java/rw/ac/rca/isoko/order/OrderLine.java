package rw.ac.rca.isoko.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_lines", schema = "cloud_sphere")
public class OrderLine {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int qty;

    @Column(name = "unit_price_rwf", nullable = false)
    private long unitPriceRwf;

    @Column(name = "shop_id") private UUID shopId;
    @Column(name = "shop_name") private String shopName;
    @Column(name = "tracking_no") private String trackingNo;
    @Column(name = "delivery_otp_hash") private String deliveryOtpHash;
    @Column(name = "shipped_at") private java.time.Instant shippedAt;
    @Column(name = "delivered_at") private java.time.Instant deliveredAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
