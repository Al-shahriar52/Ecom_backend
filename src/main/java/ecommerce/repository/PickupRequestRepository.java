package ecommerce.repository;

import ecommerce.entity.PickupRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {
    Page<PickupRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
