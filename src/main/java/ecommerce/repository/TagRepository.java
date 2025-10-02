package ecommerce.repository;

import ecommerce.dto.filter.TagFilterDTO;
import ecommerce.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Query("SELECT new ecommerce.dto.filter.TagFilterDTO(t.id, t.name, COUNT(p.id)) " +
            "FROM Product p JOIN p.tag t " +
            "WHERE (:brandId IS NULL OR p.brand.id = :brandId) " +
            "AND ((:maxPrice IS NULL AND :minPrice IS NULL) OR (p.discountedPrice BETWEEN :minPrice AND :maxPrice)) " +
            "GROUP BY t.id, t.name HAVING COUNT(p.id) > 0 ORDER BY t.name")
    List<TagFilterDTO> findTagsWithCountAndPrice(
            @Param("brandId") Long brandId,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );
}
