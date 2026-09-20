package ecommerce.repository;

import ecommerce.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("SELECT c FROM Coupon c WHERE " +
            "(:status = 'all' OR c.status = :status) AND " +
            "(:search IS NULL OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.internalName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Coupon> findFilteredCoupons(
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );

    boolean existsByCode(String code);

    Optional<Coupon> findByCode(String code);

    long countByStatus(String status);

    @Query("SELECT SUM(c.totalRedemptions) FROM Coupon c WHERE c.totalRedemptions IS NOT NULL")
    Long sumTotalRedemptions();

    List<Coupon> findByStatus(String status);
}