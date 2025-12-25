package ecommerce.service.impl;

import ecommerce.dto.WishListDto;
import ecommerce.dto.cart.CartItemDto;
import ecommerce.dto.pageResponse.WishListResponse;
import ecommerce.entity.Product;
import ecommerce.entity.User;
import ecommerce.entity.WishList;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.exceptionHandling.ResourceNotFoundException;
import ecommerce.repository.ProductRepository;
import ecommerce.repository.WishListRepository;
import ecommerce.service.WishListService;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishListServiceImpl implements WishListService {

    private final ProductRepository productRepository;
    private final WishListRepository wishListRepository;
    private final ModelMapper mapper;
    private final TokenUtil tokenUtil;

    @Override
    public Long add(Long productId, HttpServletRequest servletRequest) {

        User user = tokenUtil.extractUserInfo(servletRequest);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (wishListRepository.findByUserAndProductId(user, productId).isPresent()) {
            throw new BadRequestException("Item already in wishlist");
        }

        WishList wishlist = new WishList();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        wishListRepository.save(wishlist);

        return productId;
    }

    @Override
    public WishListResponse getAll(HttpServletRequest servletRequest) {

        User user = tokenUtil.extractUserInfo(servletRequest);

        List<WishList> wishList = wishListRepository.findByUser(user);

        List<Long> productIds = wishList.stream().map(wish -> wish.getProduct().getId()).toList();
        List<CartItemDto> productDetails = productRepository.findProductDetailsByIdIn(productIds);

        WishListResponse response = new WishListResponse();
        List<WishListDto> items = new ArrayList<>();
        for ( CartItemDto itemDto : productDetails) {
            for (WishList wish : wishList) {
                if (itemDto.getProductId().equals(wish.getProduct().getId())) {
                    WishListDto wishListDto = new WishListDto();
                    wishListDto.setWishId(wish.getId());
                    wishListDto.setPrice(itemDto.getPrice());
                    wishListDto.setProductId(itemDto.getProductId());
                    wishListDto.setProductName(itemDto.getName());
                    wishListDto.setImageUrl(itemDto.getImageUrl());
                    items.add(wishListDto);
                    break;
                }
            }
        }
        response.setItems(items);
        return response;
    }

    @Override
    @Transactional
    public Long delete(Long productId, HttpServletRequest request) {

        User user = tokenUtil.extractUserInfo(request);
        wishListRepository.deleteByUserAndProductId(user, productId);
        return productId;
    }
}
