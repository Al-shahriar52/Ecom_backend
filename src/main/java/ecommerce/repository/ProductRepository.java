package ecommerce.repository;

import ecommerce.dto.ProductSearchResponseDto;
import ecommerce.entity.PriceRange;
import ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT new ecommerce.dto.ProductSearchResponseDto(p.id, p.name, p.description, b.name, c.name, sc.name, " +
            "t.name, p.originalPrice, p.discountedPrice, p.quantity, p.sku, p.rating, p.numReviews, MIN(pi.imageUrl)) " +
            "FROM Product p " +
            "LEFT JOIN ProductImage pi on pi.product.id = p.id " +
            "LEFT JOIN Brand b on b.id=p.brand.id " +
            "LEFT JOIN Category c on c.id=p.category.id " +
            "LEFT JOIN SubCategory sc on sc.id=p.subCategory.id " +
            "LEFT JOIN Tag t on t.id=p.tag.id " +
            "WHERE p.name LIKE %:query% " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:brandId IS NULL OR b.id = :brandId) " +
            "AND (:subCategoryId IS NULL OR sc.id = :subCategoryId) " +
            "AND (:minPrice IS NULL OR p.discountedPrice >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.discountedPrice <= :maxPrice) " +
            "GROUP BY p.id, p.name, p.description, b.name, c.name, sc.name, t.name, " +
            "p.originalPrice, p.discountedPrice, p.quantity, p.sku, p.rating, p.numReviews")
    Page<ProductSearchResponseDto> search(Pageable pageable, String query, Long categoryId, Long brandId, Long subCategoryId, Double minPrice, Double maxPrice);

    @Query("SELECT new ecommerce.entity.PriceRange(MIN(p.discountedPrice), MAX(p.discountedPrice)) " +
            "FROM Product p WHERE (:brandId IS NULL OR p.brand.id = :brandId)")
    Optional<PriceRange> findPriceRangeByBrandId(@Param("brandId") Long brandId);

    @Query("SELECT new ecommerce.dto.ProductSearchResponseDto(p.id, p.name, p.description, b.name, c.name, sc.name, " +
            "t.name, p.originalPrice, p.discountedPrice, p.quantity, p.sku, p.rating, p.numReviews, MIN(pi.imageUrl)) " +
            "FROM Product p " +
            "LEFT JOIN ProductImage pi on pi.product.id = p.id " +
            "LEFT JOIN Brand b on b.id=p.brand.id " +
            "LEFT JOIN Category c on c.id=p.category.id " +
            "LEFT JOIN SubCategory sc on sc.id=p.subCategory.id " +
            "LEFT JOIN Tag t on t.id=p.tag.id " +
            "GROUP BY p.id, p.name, p.description, b.name, c.name, sc.name, t.name, " +
            "p.originalPrice, p.discountedPrice, p.quantity, p.sku, p.rating, p.numReviews" +
            " ORDER BY p.createdAt DESC " +
            "LIMIT 12")
    List<ProductSearchResponseDto> findByOrderByCreatedAtDesc();
}
