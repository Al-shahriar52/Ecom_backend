package ecommerce.repository;

import ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query(value = "select r from Review r where r.product.id = :productId")
    Page<Review> findAllReviewProductWise(Pageable pageable, Long productId);
}
