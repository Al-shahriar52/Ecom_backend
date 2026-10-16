package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.entity.Order;
import ecommerce.entity.User;
import ecommerce.entity.Role;
import ecommerce.repository.OrderRepository;
import ecommerce.service.impl.OrderTrackingService;
import ecommerce.utils.TokenUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/** Order journey with timestamps: one endpoint for the signed-in customer (own orders only) and one for admins. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderTrackingControllerImpl {

    private final OrderRepository orderRepository;
    private final OrderTrackingService trackingService;
    private final TokenUtil tokenUtil;

    @GetMapping("/order/{id}/tracking")
    @SecurityRequirement(name = "bearerAuth")
    @Transactional(readOnly = true)
    public ResponseEntity<?> myOrderTracking(HttpServletRequest request, @PathVariable Long id) {
        User user = tokenUtil.extractUserInfo(request);
        Order order = orderRepository.findById(id).orElse(null);
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.equals(Role.ADMIN));
        boolean owner = order != null && (Objects.equals(order.getUser() != null ? order.getUser().getId() : null, user.getId())
                || Objects.equals(order.getGuestUserId(), user.getId()));
        if (order == null || !(owner || isAdmin)) {
            return new ResponseEntity<>(GenericResponseDto.error("Tracking error", "Order not found.", HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
        }
        return ok(trackingService.build(order, isAdmin));
    }

    @GetMapping("/admin/orders/{id}/tracking")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> adminOrderTracking(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return new ResponseEntity<>(GenericResponseDto.error("Tracking error", "Order not found.", HttpStatus.NOT_FOUND.value()), HttpStatus.NOT_FOUND);
        }
        return ok(trackingService.build(order, true));
    }

    private ResponseEntity<?> ok(Object data) {
        return new ResponseEntity<>(GenericResponseDto.success("Tracking fetched", data, HttpStatus.OK.value()), HttpStatus.OK);
    }
}
