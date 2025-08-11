package ecommerce.controller.impl;

import ecommerce.controller.WishListController;
import ecommerce.dto.WishListDto;
import ecommerce.dto.pageResponse.WishListResponse;
import ecommerce.service.WishListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wish")
public class WishListControllerImpl implements WishListController {

    private final WishListService wishListService;

    public WishListControllerImpl(WishListService wishListService) {
        this.wishListService = wishListService;
    }

    @Override
    @PostMapping("/user/{userId}/product/{productId}/add")
    public ResponseEntity<?> add(@Valid @RequestBody WishListDto wishListDto,
                                 @PathVariable Long userId, @PathVariable Long productId) {

        WishListDto wishList = wishListService.add(wishListDto, userId, productId);
        return new ResponseEntity<>(wishList, HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/get/all")
    public ResponseEntity<?> getAll(@RequestParam(defaultValue = "0") int pageNo,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(defaultValue = "id") String sortBy) {

        WishListResponse response = wishListService.getAll(pageNo, pageSize, sortBy);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {

        String message = wishListService.delete(id);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }
}
