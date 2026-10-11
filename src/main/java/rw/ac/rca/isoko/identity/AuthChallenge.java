package rw.ac.rca.isoko.identity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Getter @Setter @Entity @Table(name="auth_challenges",schema="cloud_sphere")
public class AuthChallenge {
    @Id private UUID id=UUID.randomUUID();
    private String purpose;
    @Column(name="user_id") private UUID userId;
    private String email;
    private String phone;
    @Column(name="password_hash") private String passwordHash;
    @Column(name="code_hash") private String codeHash;
    @Column(name="guest_token") private String guestToken;
    @Column(name="expires_at") private Instant expiresAt;
    private int attempts;
    private boolean consumed;
    @Column(name="created_at") private Instant createdAt=Instant.now();
}
