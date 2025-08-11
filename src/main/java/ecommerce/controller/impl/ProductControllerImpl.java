package ecommerce.controller.impl;

import ecommerce.controller.ProductController;
import ecommerce.dto.ProductDto;
import ecommerce.dto.pageResponse.ProductResponse;
import ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/product")
public class ProductControllerImpl implements ProductController {

    private final ProductService productService;

    public ProductControllerImpl(ProductService productService) {
        this.productService = productService;
    }

    @Override
    @PostMapping("/add")
    public ResponseEntity<?> add(@Valid @RequestBody ProductDto productDto) throws IOException {

        ProductDto product = productService.add(productDto);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/get/{productId}")
    public ResponseEntity<?> getById(@PathVariable Long productId) {

            ProductDto productDto = productService.getById(productId);
            return new ResponseEntity<>(productDto, HttpStatus.OK);
    }

    @Override
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<?> delete(@PathVariable Long productId) {
        String message = productService.delete(productId);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @Override
    @PutMapping("/update/{productId}")
    public ResponseEntity<?> update(@PathVariable Long productId, @Valid @RequestBody ProductDto productDto) {

        ProductDto productUpdate = productService.update(productId,productDto);
        return new ResponseEntity<>(productUpdate, HttpStatus.OK);
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(defaultValue = "0") int pageNo,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(defaultValue = "id") String sortBy,
                                    @RequestParam(defaultValue = "") String query) {

        ProductResponse productSearch = productService.search(pageNo, pageSize, sortBy, query);
        return new ResponseEntity<>(productSearch, HttpStatus.OK);
    }
}
