package ecommerce.controller.impl;

import ecommerce.dto.admin.coupon.CouponValidateRequestDto;
import ecommerce.dto.admin.coupon.CouponValidateResponseDto;
import ecommerce.entity.Coupon;
import ecommerce.service.impl.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/checkout/coupons")
@RequiredArgsConstructor
public class CheckoutCouponController {

    private final CouponService couponService;

    @GetMapping("/available")
    public ResponseEntity<List<Coupon>> getAvailableCoupons(HttpServletRequest request) {
        List<Coupon> available = couponService.getAvailableCouponsForUser(request);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/dry-run")
    public ResponseEntity<CouponValidateResponseDto> dryRunCoupon(@RequestParam String code, HttpServletRequest request) {
        CouponValidateResponseDto response = couponService.dryRunCoupon(code, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponValidateResponseDto> validateCoupon(@RequestBody CouponValidateRequestDto request) {
        CouponValidateResponseDto response = couponService.validateAndCalculateCoupon(request);
        return ResponseEntity.ok(response);
    }
}