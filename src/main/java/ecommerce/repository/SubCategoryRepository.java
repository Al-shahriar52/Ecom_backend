package ecommerce.repository;

import ecommerce.dto.SubCategoryDto;
import ecommerce.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    @Query("select new ecommerce.dto.SubCategoryDto(sc.id, sc.name) from SubCategory sc where sc.category.id =:categoryId")
    List<SubCategoryDto> findByCategoryId(@Param("categoryId") Long categoryId);
}
