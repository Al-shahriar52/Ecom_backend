package ecommerce.repository;

import ecommerce.dto.filter.CategoryFilterDTO;
import ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT new ecommerce.dto.filter.CategoryFilterDTO(c.id, c.name, COUNT(p.id)) " +
            "FROM Product p JOIN p.category c " +
            "WHERE (:brandId IS NULL OR p.brand.id = :brandId) " +
            "AND ((:maxPrice IS NULL AND :minPrice IS NULL) OR (p.discountedPrice BETWEEN :minPrice AND :maxPrice)) " +
            "GROUP BY c.id, c.name HAVING COUNT(p.id) > 0 ORDER BY c.name")
    List<CategoryFilterDTO> findCategoriesWithCountAndPrice(
            @Param("brandId") Long brandId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );
}
