package rw.ac.rca.isoko.seller;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @Entity @Table(name = "shops", schema = "cloud_sphere")
public class Shop {
    @Id private UUID id = UUID.randomUUID();
    @Column(name = "owner_id", nullable = false, unique = true) private UUID ownerId;
    @Column(nullable = false) private String name;
    private String description;
    @Column(name = "business_reference", nullable = false) private String businessReference;
    @Enumerated(EnumType.STRING) private Status status = Status.PENDING;
    @Column(name = "review_note") private String reviewNote;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    public enum Status { PENDING, APPROVED, REJECTED, SUSPENDED }
}
