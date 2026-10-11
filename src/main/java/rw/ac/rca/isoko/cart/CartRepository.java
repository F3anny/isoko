package rw.ac.rca.isoko.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    @Query("select distinct c from Cart c left join fetch c.items where c.userId = :userId")
    Optional<Cart> findByUserId(UUID userId);

    @Query("select distinct c from Cart c left join fetch c.items where c.guestToken = :guestToken")
    Optional<Cart> findByGuestToken(String guestToken);
}
