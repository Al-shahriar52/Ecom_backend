package ecommerce.controller.impl;

import ecommerce.controller.ReviewController;
import ecommerce.dto.ReviewDto;
import ecommerce.dto.pageResponse.ReviewResponse;
import ecommerce.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/review")
public class ReviewControllerImpl implements ReviewController {

    private final ReviewService reviewService;

    public ReviewControllerImpl(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Override
    @PostMapping("/user/{userId}/product/{productId}/add")
    public ResponseEntity<?> add(@Valid @RequestBody ReviewDto reviewDto,
                                 @PathVariable Long userId,
                                 @PathVariable Long productId) {

        ReviewDto review = reviewService.add(reviewDto, productId, userId);
        return new ResponseEntity<>(review, HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/product/{productId}/all")
    public ResponseEntity<?> findAllReviewProductWise(@RequestParam(defaultValue = "0") int pageNo,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(defaultValue = "id") String sortBy,
                                                      @PathVariable Long productId) {

        ReviewResponse response = reviewService.findAllReviewByProductWise(pageNo, pageSize, sortBy, productId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
