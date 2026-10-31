package ecommerce.repository;

import ecommerce.dto.ProductSearchResponseDto;
import ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Finds products for the home page "Shop by concern" cards (keyword match, drafts excluded). */
public interface ConcernProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT new ecommerce.dto.ProductSearchResponseDto(p.id, p.name, p.description, b.name, c.name, sc.name, " +
            "t.name, p.originalPrice, p.discountedPrice, p.quantity, p.sku, p.rating, p.numReviews, MIN(pi.imageUrl), p.slug, MIN(v.size)) " +
            "FROM Product p " +
            "LEFT JOIN ProductImage pi on pi.product.id = p.id " +
            "LEFT JOIN Variation v on v.product.id = p.id " +
            "LEFT JOIN Brand b on b.id=p.brand.id " +
            "LEFT JOIN Category c on c.id=p.category.id " +
            "LEFT JOIN SubCategory sc on sc.id=p.subCategory.id " +
            "LEFT JOIN Tag t on t.id=p.tag.id " +
            "WHERE (p.status is null or p.status <> ecommerce.entity.ProductStatus.DRAFT) " +
            "AND (LOWER(p.name) LIKE :k1 OR LOWER(p.description) LIKE :k1 OR LOWER(sc.name) LIKE :k1 OR LOWER(t.name) LIKE :k1 " +
            "  OR LOWER(p.name) LIKE :k2 OR LOWER(p.description) LIKE :k2 OR LOWER(sc.name) LIKE :k2 OR LOWER(t.name) LIKE :k2 " +
            "  OR LOWER(p.name) LIKE :k3 OR LOWER(p.description) LIKE :k3 OR LOWER(sc.name) LIKE :k3 OR LOWER(t.name) LIKE :k3 " +
            "  OR LOWER(p.name) LIKE :k4 OR LOWER(p.description) LIKE :k4 OR LOWER(sc.name) LIKE :k4 OR LOWER(t.name) LIKE :k4 " +
            "  OR LOWER(p.name) LIKE :k5 OR LOWER(p.description) LIKE :k5 OR LOWER(sc.name) LIKE :k5 OR LOWER(t.name) LIKE :k5) " +
            "GROUP BY p.id, p.name, p.description, b.name, c.name, sc.name, t.name, " +
            "p.originalPrice, p.discountedPrice, p.quantity, p.sku, p.rating, p.numReviews, p.slug")
    Page<ProductSearchResponseDto> findByKeywords(Pageable pageable,
                                                  @Param("k1") String k1, @Param("k2") String k2, @Param("k3") String k3,
                                                  @Param("k4") String k4, @Param("k5") String k5);
}
