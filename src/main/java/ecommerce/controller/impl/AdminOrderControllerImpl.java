package ecommerce.controller.impl;

import ecommerce.controller.AdminOrderController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.admin.AdminOrderListDTO;
import ecommerce.dto.admin.CancelOrderRequest;
import ecommerce.dto.admin.OrderStatsDTO;
import ecommerce.dto.admin.steadfast.request.PickupRequest;
import ecommerce.service.AdminOrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderControllerImpl implements AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "All") String method,
            @RequestParam(required = false, defaultValue = "All") String paymentStatus,
            @RequestParam(required = false, defaultValue = "All") String orderStatus,
            @RequestParam(required = false, defaultValue = "All") String deliveryStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        Page<AdminOrderListDTO> result = adminOrderService.getAdminOrders(
                page, size, search, method, paymentStatus, orderStatus, deliveryStatus, startDate, endDate
        );

        return new ResponseEntity<>(GenericResponseDto.success("Order fetched successfully", result, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/stats")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOrderStats() {
        return new ResponseEntity<>(GenericResponseDto.success("Statistics", adminOrderService.getOrderStats(), HttpStatus.OK.value()), HttpStatus.OK);
    }

    @PostMapping("/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cancelOrders(@RequestBody CancelOrderRequest request) {
        adminOrderService.cancelOrders(request.getInvoices());
        // Returning a standard JSON response object
        return new ResponseEntity<>(GenericResponseDto.success("Orders canceled successfully", null, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @PostMapping("/pickup")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> requestSteadfastPickup(@RequestBody PickupRequest request) {
            adminOrderService.requestParcelPickup(request.getOrderIds());
            return new ResponseEntity<>(GenericResponseDto.success("Order pickup request successfully", null, HttpStatus.OK.value()), HttpStatus.OK);
    }
}