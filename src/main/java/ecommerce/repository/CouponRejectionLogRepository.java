package ecommerce.repository;

import ecommerce.entity.CouponRejectionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CouponRejectionLogRepository extends JpaRepository<CouponRejectionLog, Long> {
    List<CouponRejectionLog> findByCouponCode(String couponCode);
}