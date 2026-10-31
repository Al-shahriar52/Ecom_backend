package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.admin.create.AdminCreateOrderRequest;
import ecommerce.service.impl.AdminOrderCreateService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/order-builder")
@RequiredArgsConstructor
@PreAuthorize("@perm.has(authentication, 14)")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderCreateController {

    private final AdminOrderCreateService service;

    @GetMapping("/customers")
    public ResponseEntity<?> customers(@RequestParam String q) {
        return ResponseEntity.ok(GenericResponseDto.success("Customers", service.searchCustomers(q), HttpStatus.OK.value()));
    }

    @GetMapping("/products")
    public ResponseEntity<?> products(@RequestParam String q) {
        return ResponseEntity.ok(GenericResponseDto.success("Products", service.searchProducts(q), HttpStatus.OK.value()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AdminCreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponseDto.success("Order created", service.createOrder(request), HttpStatus.CREATED.value()));
    }
}
