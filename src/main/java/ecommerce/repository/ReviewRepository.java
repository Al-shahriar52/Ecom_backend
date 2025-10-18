package ecommerce.repository;

import ecommerce.dto.ReviewResponseDto;
import ecommerce.dto.pageResponse.ReviewResponse;
import ecommerce.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query(value = "select new ecommerce.dto.ReviewResponseDto(r.name, r.rating, r.comment, r.createdAt, r.imageUrl) from Review r " +
            "where r.product.id = :productId " +
            "AND r.isApproved = true")
    Page<ReviewResponseDto> findAllReviewProductWise(Pageable pageable, Long productId);
}
