package ecommerce.repository;

import ecommerce.entity.HomeBanner;
import ecommerce.enums.BannerPlacement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeBannerRepository extends JpaRepository<HomeBanner, Long> {
    List<HomeBanner> findByPlacementOrderBySortOrderAscIdAsc(BannerPlacement placement);
    List<HomeBanner> findAllByOrderByPlacementAscSortOrderAscIdAsc();
    long countByPlacement(BannerPlacement placement);
}
