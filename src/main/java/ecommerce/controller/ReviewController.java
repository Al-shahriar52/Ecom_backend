package ecommerce.controller;

import ecommerce.dto.ReviewDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ReviewController {

    ResponseEntity<?> add(String reviewDto, HttpServletRequest servletRequest, MultipartFile file) throws IOException;
    ResponseEntity<?> findAllReviewProductWise(int pageNo, int pageSize, String sortBy, Long productId);
}
