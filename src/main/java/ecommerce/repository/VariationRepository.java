package ecommerce.repository;

import ecommerce.entity.Variation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariationRepository extends JpaRepository<Variation, Long> {
    List<Variation> findByProductId(Long productId);
}
