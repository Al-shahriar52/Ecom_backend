package ecommerce.repository;

import ecommerce.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    java.util.List<WebhookEvent> findTop30ByOrderByReceivedAtDesc();

    boolean existsByProviderAndIdempotencyKey(String provider, String idempotencyKey);
}
