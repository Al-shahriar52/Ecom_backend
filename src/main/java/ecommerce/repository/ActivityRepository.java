package ecommerce.repository;

import ecommerce.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    // Fetches a user's activity log, sorted with the newest actions first
    List<Activity> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}