package ecommerce.service;

import ecommerce.dto.ReviewDto;
import ecommerce.dto.pageResponse.ReviewResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ReviewService {

    ReviewDto add(ReviewDto reviewDto, HttpServletRequest servletRequest, MultipartFile files) throws IOException;
    ReviewResponse findAllReviewByProductWise(int pageNo, int pageSize, String sortBy, Long productId);
}
