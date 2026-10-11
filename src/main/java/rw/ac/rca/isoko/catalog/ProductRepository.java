package rw.ac.rca.isoko.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    java.util.List<Product> findByShopId(UUID shopId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> lockById(@Param("id") UUID id);
    @Query("select p from Product p left join p.shop s where p.status = 'PUBLISHED' and (s is null or s.status = 'APPROVED') and (lower(p.name) like lower(concat('%', :q, '%')) or lower(p.sku) like lower(concat('%', :q, '%')) or lower(coalesce(p.description,'')) like lower(concat('%', :q, '%')))")
    Page<Product> searchPublished(@Param("q") String q, Pageable pageable);
    Optional<Product> findBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCase(String sku);

    Page<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String name, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Optional<Product> findByIdAndStatus(UUID id, ProductStatus status);

    @Modifying
    @Query("""
            update Product p set p.stockQty = p.stockQty - :qty
            where p.id = :id and p.stockQty >= :qty
            """)
    int decrementStock(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying
    @Query("update Product p set p.stockQty = p.stockQty + :qty where p.id = :id")
    int incrementStock(@Param("id") UUID id, @Param("qty") int qty);
}
