package ecommerce.controller.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.controller.ReviewController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.ReviewDto;
import ecommerce.dto.pageResponse.ReviewResponse;
import ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.GeneratedValue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewControllerImpl implements ReviewController {

    private final ReviewService reviewService;

    public ReviewControllerImpl(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Override
    @PostMapping(value = "/add", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> add(@Valid @RequestPart String reviewDtoString,
                                 HttpServletRequest servletRequest,
                                 @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ReviewDto reviewDto = mapper.readValue(reviewDtoString, ReviewDto.class);
        ReviewDto review = reviewService.add(reviewDto, servletRequest, file);
        return new ResponseEntity<>(GenericResponseDto.success("Review created successfully", review, HttpStatus.CREATED.value()), HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> findAllReviewProductWise(@RequestParam(defaultValue = "0") int pageNo,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(defaultValue = "id") String sortBy,
                                                      @PathVariable Long productId) {

        ReviewResponse response = reviewService.findAllReviewByProductWise(pageNo, pageSize, sortBy, productId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
