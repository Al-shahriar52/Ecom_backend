package ecommerce.controller.impl;

import ecommerce.controller.WishListController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.pageResponse.WishListResponse;
import ecommerce.service.WishListService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishListControllerImpl implements WishListController {

    private final WishListService wishListService;

    @Override
    @PostMapping("/add/{productId}")
    public ResponseEntity<?> add(@PathVariable Long productId, HttpServletRequest servletRequest) {
        Long wishList = wishListService.add(productId, servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Wishlist added successfully", wishList, HttpStatus.CREATED.value()), HttpStatus.CREATED);
    }

    @Override
    @GetMapping()
    public ResponseEntity<?> getAll(HttpServletRequest servletRequest) {

        WishListResponse response = wishListService.getAll(servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Wishlist fetch successfully", response, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> delete(@PathVariable Long productId, HttpServletRequest servletRequest) {

        Long response = wishListService.delete(productId, servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Wish item remove successfully", response, HttpStatus.OK.value()), HttpStatus.OK);
    }
}
