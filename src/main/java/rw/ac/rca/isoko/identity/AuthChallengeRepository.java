package rw.ac.rca.isoko.identity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.Instant;
public interface AuthChallengeRepository extends JpaRepository<AuthChallenge,UUID> {
    long countByPhoneAndCreatedAtAfter(String phone,Instant since);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from AuthChallenge c where c.id=:id")
    Optional<AuthChallenge> lockById(@Param("id") UUID id);
}
