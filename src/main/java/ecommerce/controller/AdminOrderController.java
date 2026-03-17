package ecommerce.controller;

import ecommerce.dto.admin.CancelOrderRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

public interface AdminOrderController {

    ResponseEntity<?> getAllOrders(
            int page,
            int size,
            String search,
            String method,
            String paymentStatus,
            String orderStatus,
            String deliveryStatus,
            LocalDateTime startDate,
            LocalDateTime endDate);

    ResponseEntity<?> getOrderStats();

    ResponseEntity<?> cancelOrders(@RequestBody CancelOrderRequest request);
}
