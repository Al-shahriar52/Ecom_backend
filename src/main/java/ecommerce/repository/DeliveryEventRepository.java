package ecommerce.repository;

import ecommerce.entity.DeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, Long> {
    List<DeliveryEvent> findByDeliveryIdOrderByOccurredAtAsc(Long deliveryId);
}
