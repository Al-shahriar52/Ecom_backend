package ecommerce.repository;

import ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Product counts per brand / category / sub-category, for the admin catalog page. */
public interface CatalogCountRepository extends JpaRepository<Product, Long> {

    @Query("select p.brand.id, count(p) from Product p where p.brand is not null group by p.brand.id")
    List<Object[]> brandCounts();

    @Query("select p.category.id, count(p) from Product p where p.category is not null group by p.category.id")
    List<Object[]> categoryCounts();

    @Query("select p.subCategory.id, count(p) from Product p where p.subCategory is not null group by p.subCategory.id")
    List<Object[]> subCategoryCounts();

    @Query("select count(p) from Product p where p.brand.id = :id")
    long countBrand(@Param("id") Long id);

    @Query("select count(p) from Product p where p.category.id = :id")
    long countCategory(@Param("id") Long id);

    @Query("select count(p) from Product p where p.subCategory.id = :id")
    long countSubCategory(@Param("id") Long id);
}
