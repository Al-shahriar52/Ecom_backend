package ecommerce.service;

import ecommerce.dto.ReviewDto;
import ecommerce.dto.pageResponse.ReviewResponse;

public interface ReviewService {

    ReviewDto add(ReviewDto reviewDto, Long productId, Long userId);
    ReviewResponse findAllReviewByProductWise(int pageNo, int pageSize, String sortBy, Long productId);
}
