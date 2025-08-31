package ecommerce.repository;

import ecommerce.dto.ProductSearchResponseDto;
import ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
            "AND (:category IS NULL OR :category = '' OR c.name = :category) " +
            "GROUP BY p.id, p.name, p.description, b.name, c.name, sc.name, t.name, " +
            "p.originalPrice, p.discountedPrice, p.quantity, p.sku, p.rating, p.numReviews")
    Page<ProductSearchResponseDto> search(Pageable pageable, String query, String category);

}
