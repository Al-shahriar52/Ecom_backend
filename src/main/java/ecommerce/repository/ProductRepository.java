package ecommerce.repository;

import ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p from Product p where p.name like concat('%',:query,'%') " +
            "or p.brand like concat('%',:query,'%') " +
            "or p.category like concat('%',:query,'%') " +
            "or p.description like concat('%',:query,'%') ")
    Page<Product> search(Pageable pageable, String query);
}
