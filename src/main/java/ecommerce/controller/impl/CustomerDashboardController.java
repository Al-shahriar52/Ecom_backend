package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.entity.User;
import ecommerce.service.impl.CustomerDashboardService;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** GET /api/v1/account/summary - the signed-in customer's own numbers (needs a login, nothing else). */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class CustomerDashboardController {

    private final CustomerDashboardService service;
    private final TokenUtil tokenUtil;

    @GetMapping("/summary")
    public ResponseEntity<?> summary(HttpServletRequest request) {
        User user = tokenUtil.extractUserInfo(request);
        return ResponseEntity.ok(GenericResponseDto.success("Account summary", service.summary(user), HttpStatus.OK.value()));
    }
}
