package ecommerce.repository;

import ecommerce.entity.PickupLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PickupLocationRepository extends JpaRepository<PickupLocation, Long> {
    List<PickupLocation> findByActiveTrueOrderByPrimaryLocationDescLabelAsc();
    Optional<PickupLocation> findByIdAndActiveTrue(Long id);
}
