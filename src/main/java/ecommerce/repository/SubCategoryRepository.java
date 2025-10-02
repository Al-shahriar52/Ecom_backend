package ecommerce.repository;

import ecommerce.dto.SubCategoryDto;
import ecommerce.dto.filter.SubCategoryFilterDTO;
import ecommerce.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    @Query("select new ecommerce.dto.SubCategoryDto(sc.id, sc.name) from SubCategory sc where sc.category.id =:categoryId")
    List<SubCategoryDto> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT new ecommerce.dto.filter.SubCategoryFilterDTO(sc.id, sc.name, COUNT(p.id)) " +
            "FROM Product p JOIN p.subCategory sc " +
            "WHERE sc.category.id = :categoryId " +
            "AND (:brandId IS NULL OR p.brand.id = :brandId) " + // <-- UPDATED
            "AND p.discountedPrice BETWEEN :minPrice AND :maxPrice " +
            "GROUP BY sc.id, sc.name HAVING COUNT(p.id) > 0 ORDER BY sc.name")
    List<SubCategoryFilterDTO> findSubCategoriesWithCount(
            @Param("brandId") Long brandId,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );
}

