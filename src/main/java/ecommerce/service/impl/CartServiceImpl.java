package ecommerce.service.impl;

import ecommerce.dto.cart.AddToCartRequestDto;
import ecommerce.dto.cart.CartItemDto;
import ecommerce.dto.cart.CartItemListDto;
import ecommerce.dto.cart.CartUpdateRequestDto;
import ecommerce.entity.Cart;
import ecommerce.entity.CartItem;
import ecommerce.entity.Product;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.exceptionHandling.ResourceNotFoundException;
import ecommerce.repository.CartItemRepository;
import ecommerce.repository.CartRepository;
import ecommerce.repository.ProductRepository;
import ecommerce.service.CartService;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private final ProductRepository productRepository;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final TokenUtil tokenUtil;

    public CartServiceImpl(ProductRepository productRepository, CartRepository cartRepository,
                           CartItemRepository cartItemRepository, TokenUtil tokenUtil) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.tokenUtil = tokenUtil;
    }

    @Override
    @Transactional
    public AddToCartRequestDto addItemToCart(AddToCartRequestDto addItemDTO, HttpServletRequest servletRequest) {
        Cart cart = getOrCreateCartOfCurrentUser(servletRequest);

        Product product = productRepository.findById(addItemDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if the item is already in the cart
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + addItemDTO.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            newItem.setQuantity(addItemDTO.getQuantity());
            newItem.setCart(cart);
            newItem.setActive(true);
            cartItemRepository.save(newItem);
        }

        return addItemDTO;
    }

    private Cart getOrCreateCartOfCurrentUser(HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);
        // Find the cart by user. If it doesn't exist, create a new one.
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            newCart.setActive(true);
            return cartRepository.save(newCart);
        });
    }



    @Override
    public CartItemListDto getCart(HttpServletRequest servletRequest) {

        User user = tokenUtil.extractUserInfo(servletRequest);

        Cart cart = cartRepository.findByUser(user).orElseThrow(() ->
                new ResourceNotFound("Cart", "user", user.getId()));

        List<CartItem> cartItems = cartItemRepository.findByCartAndIsActiveTrue(cart);
        List<Long> productIds = cartItems.stream()
                .map(item -> item.getProduct().getId())
                .toList();
        List<CartItemDto> productDetails = productRepository.findProductDetailsByIdIn(productIds);

        CartItemListDto cartItemListDto = new CartItemListDto();
        for ( CartItemDto itemDto : productDetails) {
            for (CartItem cartItem : cartItems) {
                if (itemDto.getProductId().equals(cartItem.getProduct().getId())) {
                    itemDto.setQuantity(cartItem.getQuantity());
                    itemDto.setItemTotalPrice(itemDto.getPrice() * cartItem.getQuantity());
                    itemDto.setCartItemId(cartItem.getId());
                    break;
                }
            }
        }

        double totalPrice = productDetails.stream()
                .mapToDouble(CartItemDto::getItemTotalPrice)
                .sum();
        cartItemListDto.setTotalPrice(totalPrice);
        cartItemListDto.setItems(productDetails);
        return cartItemListDto;
    }

    @Override
    public Long delete(Long cartItemId, HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);

        CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(() ->
                new ResourceNotFound("Cart Item", "id", cartItemId));
        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Cart Item not found for the user");
        }
        cartItemRepository.delete(cartItem);
        return cartItemId;
    }

    @Override
    public CartUpdateRequestDto updateQuantity(CartUpdateRequestDto requestDto, HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);

        CartItem cartItem = cartItemRepository.findById(requestDto.getCartItemId()).orElseThrow(() ->
                new ResourceNotFound("Cart Item", "id", requestDto.getCartItemId()));
        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Cart Item not found for the user");
        }
        cartItem.setQuantity(requestDto.getQuantity());
        cartItemRepository.save(cartItem);
        return requestDto;
    }
}
