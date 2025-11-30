package ecommerce.service.impl;

import ecommerce.dto.ReviewDto;
import ecommerce.dto.ReviewResponseDto;
import ecommerce.dto.pageResponse.ReviewResponse;
import ecommerce.entity.Product;
import ecommerce.entity.Review;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.ProductRepository;
import ecommerce.repository.ReviewRepository;
import ecommerce.repository.UserRepository;
import ecommerce.service.ReviewService;
import ecommerce.utils.DateTimeUtil;
import ecommerce.utils.ImageUtil;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ModelMapper mapper;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final TokenUtil tokenUtil;
    private final ImageUtil imageUtil;

    public ReviewServiceImpl(ModelMapper mapper,
                             ReviewRepository reviewRepository,
                             ProductRepository productRepository,
                             TokenUtil tokenUtil,
                             ImageUtil imageUtil) {
        this.mapper = mapper;
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.tokenUtil = tokenUtil;
        this.imageUtil = imageUtil;
    }

    @Override
    public ReviewDto add(ReviewDto reviewDto, HttpServletRequest servletRequest, MultipartFile file) throws IOException {

        User userInfo = tokenUtil.extractUserInfo(servletRequest);

        Product product = productRepository.findById(reviewDto.getProductId()).orElseThrow(()->
                new ResourceNotFound("Product", "id", reviewDto.getProductId()));

        Review review = new Review();
        review.setComment(reviewDto.getComment());
        review.setRating(reviewDto.getRating());

        if (file != null) {
            String imageUrl = imageUtil.uploadFile(file);
            review.setImageUrl(imageUrl);
        }
        review.setProduct(product);

        if (userInfo.getPhone() != null) {
            review.setName(userInfo.getName() != null ? userInfo.getName() : userInfo.getPhone());
        }else {
            review.setName(userInfo.getName() != null ? userInfo.getName() : userInfo.getEmail());
        }

        review.setUser(userInfo);
        reviewRepository.save(review);

        Long numReviews = product.getNumReviews();
        double rating = product.getRating();
        double newRating = ((rating * numReviews) + reviewDto.getRating()) / (numReviews + 1);

        product.setRating(newRating);
        product.setNumReviews(numReviews + 1);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        return mapToDto(review);
    }

    @Override
    public ReviewResponse findAllReviewByProductWise(int pageNo, int pageSize,
                                                     String sortBy, Long productId) {

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<ReviewResponseDto> reviews = reviewRepository.findAllReviewProductWise(pageable, productId);

        List<ReviewResponseDto> reviewList = reviews.getContent();

        ReviewResponse response = new ReviewResponse();
        response.setContent(reviewList);
        response.setPageNo(reviews.getNumber());
        response.setPageSize(reviews.getSize());
        response.setTotalPages(reviews.getTotalPages());
        response.setTotalElements(reviews.getTotalElements());
        response.setLast(reviews.isLast());

        return response;
    }

    public Review mapToEntity(ReviewDto reviewDto) {
        return mapper.map(reviewDto, Review.class);
    }

    public ReviewDto mapToDto(Review review) {
        return mapper.map(review, ReviewDto.class);
    }
}
