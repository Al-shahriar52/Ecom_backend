package ecommerce.repository;

import ecommerce.dto.AllBrand;
import ecommerce.dto.BrandMenuDTO;
import ecommerce.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    @Query("SELECT new ecommerce.dto.BrandMenuDTO(b.id, b.name, b.slug, COUNT(p.id), b.isTop, b.logoUrl) " +
            "FROM Brand b LEFT JOIN Product p ON b.id = p.brand.id " +
            "GROUP BY b.id, b.name, b.slug, b.isTop, b.logoUrl " +
            "ORDER BY b.name ASC")
    List<BrandMenuDTO> findAllBrandMenuData();
}
