package ecommerce.controller;

import ecommerce.dto.ReviewDto;
import org.springframework.http.ResponseEntity;

public interface ReviewController {

    ResponseEntity<?> add(ReviewDto reviewDto, Long userId, Long productId);
    ResponseEntity<?> findAllReviewProductWise(int pageNo, int pageSize, String sortBy, Long productId);
}
