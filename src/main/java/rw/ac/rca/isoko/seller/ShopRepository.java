package rw.ac.rca.isoko.seller;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface ShopRepository extends JpaRepository<Shop, UUID> {
    Optional<Shop> findByOwnerId(UUID ownerId);
}
