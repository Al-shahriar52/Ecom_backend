package ecommerce.service.impl;

import ecommerce.dto.ReviewDto;
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
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ModelMapper mapper;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final DateTimeUtil dateTimeUtil;

    public ReviewServiceImpl(ModelMapper mapper, ReviewRepository reviewRepository,
                             UserRepository userRepository,
                             ProductRepository productRepository, DateTimeUtil dateTimeUtil) {
        this.mapper = mapper;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.dateTimeUtil = dateTimeUtil;
    }

    @Override
    public ReviewDto add(ReviewDto reviewDto, Long productId, Long userId) {

        Review review = mapToEntity(reviewDto);

        Product product = productRepository.findById(productId).orElseThrow(()->
                new ResourceNotFound("Product", "id", productId));

        User user = userRepository.findById(userId).orElseThrow(()->
                new ResourceNotFound("User", "id", userId));

        String createdAt = dateTimeUtil.convert();
        review.setProduct(product);
        review.setName(user.getName());
        review.setUser(user);
        review.setCreatedAt(createdAt);
        reviewRepository.save(review);

        return mapToDto(review);
    }

    @Override
    public ReviewResponse findAllReviewByProductWise(int pageNo, int pageSize,
                                                     String sortBy, Long productId) {

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<Review> reviews = reviewRepository.findAllReviewProductWise(pageable, productId);

        List<Review> reviewList = reviews.getContent();
        List<ReviewDto> content = reviewList.stream().map(this::mapToDto).toList();

        ReviewResponse response = new ReviewResponse();
        response.setContent(content);
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
