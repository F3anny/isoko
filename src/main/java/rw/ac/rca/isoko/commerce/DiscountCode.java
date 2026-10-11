package rw.ac.rca.isoko.commerce;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "discount_codes", schema = "cloud_sphere")
public class DiscountCode {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "percent_off", nullable = false)
    private int percentOff;

    @Column(name = "min_qty", nullable = false)
    private int minQty = 1;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    public boolean isUsable(Instant now, int qty) {
        if (!active) {
            return false;
        }
        if (qty < minQty) {
            return false;
        }
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || !now.isAfter(validUntil);
    }
}
