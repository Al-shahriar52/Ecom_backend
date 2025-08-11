package ecommerce.service.impl;

import ecommerce.dto.WishListDto;
import ecommerce.dto.pageResponse.WishListResponse;
import ecommerce.entity.Product;
import ecommerce.entity.User;
import ecommerce.entity.WishList;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.ProductRepository;
import ecommerce.repository.UserRepository;
import ecommerce.repository.WishListRepository;
import ecommerce.service.WishListService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishListServiceImpl implements WishListService {

    private final UserRepository userRepository;

    private final ProductRepository productRepository;

    private final WishListRepository wishListRepository;

    private final ModelMapper mapper;

    public WishListServiceImpl(UserRepository userRepository, ProductRepository productRepository,
                               WishListRepository wishListRepository, ModelMapper mapper) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.wishListRepository = wishListRepository;
        this.mapper = mapper;
    }

    @Override
    public WishListDto add(WishListDto wishListDto, Long userId, Long productId) {

        WishList wishList = mapToEntity(wishListDto);

        User user = userRepository.findById(userId).orElseThrow(()->
                new ResourceNotFound("User", "id", userId));

        Product product = productRepository.findById(productId).orElseThrow(()->
                new ResourceNotFound("Product", "id", productId));

        wishList.setUser(user);
        wishList.setProduct(product);
        wishListRepository.save(wishList);

        return mapToDto(wishList);
    }

    @Override
    public WishListResponse getAll(int pageNo, int pageSize, String sortBy) {

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<WishList> wishLists = wishListRepository.findAll(pageable);

        List<WishList> lists = wishLists.getContent();
        List<WishListDto> content = lists.stream().map(this::mapToDto).toList();

        WishListResponse response = new WishListResponse();
        response.setContent(content);
        response.setPageNo(wishLists.getNumber());
        response.setPageSize(wishLists.getSize());
        response.setTotalPages(wishLists.getTotalPages());
        response.setTotalElements(wishLists.getTotalElements());
        response.setLast(wishLists.isLast());

        return response;
    }

    @Override
    public String delete(Long id) {

        WishList wishList = wishListRepository.findById(id).orElseThrow(()->
                new ResourceNotFound("WishList", "id", id));

        wishListRepository.delete(wishList);
        return "Your wish item name : "+wishList.getProduct().getName()+"is successfully deleted.";
    }

    public WishList mapToEntity(WishListDto wishListDto) {
        return mapper.map(wishListDto, WishList.class);
    }

    public WishListDto mapToDto(WishList wishList) {
        return mapper.map(wishList, WishListDto.class);
    }
}
