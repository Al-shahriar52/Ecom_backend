package ecommerce.controller.impl;

import ecommerce.controller.CartController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.cart.AddToCartRequestDto;
import ecommerce.dto.cart.CartItemListDto;
import ecommerce.dto.cart.CartUpdateRequestDto;
import ecommerce.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartControllerImpl implements CartController {

    private final CartService cartService;

    public CartControllerImpl(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    @PostMapping("/addToCart")
    public ResponseEntity<?> add(@Valid @RequestBody AddToCartRequestDto requestDto, HttpServletRequest servletRequest) {

        AddToCartRequestDto cart = cartService.addItemToCart(requestDto, servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Item added successfully", cart, HttpStatus.OK.value()), HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/getCart")
    public ResponseEntity<?> getCart(HttpServletRequest servletRequest) {

        CartItemListDto response = cartService.getCart(servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Cart Item fetched successfully", response, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/delete/{cartItemId}")
    public ResponseEntity<?> delete(@PathVariable Long cartItemId, HttpServletRequest servletRequest) {

        Long response = cartService.delete(cartItemId, servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Cart Item deleted successfully", response, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @Override
    @PutMapping("/update")
    public ResponseEntity<?> updateQuantity(@Valid @RequestBody CartUpdateRequestDto requestDto, HttpServletRequest servletRequest) {
        CartUpdateRequestDto response = cartService.updateQuantity(requestDto, servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Cart Item updated successfully", response, HttpStatus.OK.value()), HttpStatus.OK);
    }
}
