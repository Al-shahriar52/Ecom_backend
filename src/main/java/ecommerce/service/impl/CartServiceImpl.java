package ecommerce.service.impl;

import ecommerce.dto.CartDto;
import ecommerce.dto.pageResponse.CartResponse;
import ecommerce.entity.Cart;
import ecommerce.entity.Product;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.CartRepository;
import ecommerce.repository.ProductRepository;
import ecommerce.repository.UserRepository;
import ecommerce.service.CartService;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final UserRepository userRepository;

    private final ProductRepository productRepository;
    private final ModelMapper mapper;

    private final CartRepository cartRepository;

    public CartServiceImpl(UserRepository userRepository, ProductRepository productRepository,
                           ModelMapper mapper, CartRepository cartRepository) {

        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.cartRepository = cartRepository;
    }

    @Override
    public CartDto add(CartDto cartDto, Long userId, Long productId) {

        Cart cart = mapToEntity(cartDto);
        User user = userRepository.findById(userId).orElseThrow(()->
                new ResourceNotFound("User", "id", userId));

        Product product = productRepository.findById(productId).orElseThrow(()->
                new ResourceNotFound("Product", "id", productId));

        cart.setUser(user);
        cart.setProduct(product);
        Cart saveCart = cartRepository.save(cart);

        return mapToDto(saveCart);
    }

    @Override
    public CartResponse getAll(int pageNo, int pageSize, String sortBy) {

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<Cart> carts = cartRepository.findAll(pageable);

        List<Cart> cartList = carts.getContent();
        List<CartDto> content = cartList.stream().map(this::mapToDto).toList();

        CartResponse response = new CartResponse();
        response.setContent(content);
        response.setPageNo(carts.getNumber());
        response.setPageSize(carts.getSize());
        response.setTotalPages(carts.getTotalPages());
        response.setTotalElements(carts.getTotalElements());
        response.setLast(carts.isLast());

        return response;
    }

    @Override
    public String delete(Long cartId) {
        Cart cart = cartRepository.findById(cartId).orElseThrow(()->
                new ResourceNotFound("Cart", "id", cartId));

        cartRepository.delete(cart);
        return "Your cart item name : "+cart.getProduct().getName()+" is successfully deleted.";
    }

    public Cart mapToEntity(CartDto cartDto) {

        return mapper.map(cartDto, Cart.class);
    }

    public CartDto mapToDto(Cart cart) {

        return mapper.map(cart, CartDto.class);
    }
}
