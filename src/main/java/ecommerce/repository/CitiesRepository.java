package ecommerce.repository;

import ecommerce.entity.Cities;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitiesRepository extends JpaRepository<Cities, Long> {
}
