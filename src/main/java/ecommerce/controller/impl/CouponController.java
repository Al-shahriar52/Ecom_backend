package ecommerce.controller.impl;

import ecommerce.dto.admin.coupon.CouponCreateRequestDto;
import ecommerce.dto.admin.coupon.CouponValidateRequestDto;
import ecommerce.dto.admin.coupon.CouponValidateResponseDto;
import ecommerce.entity.Coupon;
import ecommerce.repository.CouponRepository;
import ecommerce.service.impl.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponRepository couponRepository;
    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<Page<Coupon>> getCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "recent") String sortBy
    ) {
        Sort sort = sortBy.equals("code") ? Sort.by("code").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Coupon> couponPage = couponRepository.findFilteredCoupons(status, search, pageable);
        return ResponseEntity.ok(couponPage);
    }

    @PostMapping
    public ResponseEntity<Coupon> createCoupon(@RequestBody CouponCreateRequestDto request) {
        Coupon createdCoupon = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCoupon);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Coupon> getCouponById(@PathVariable Long id) {
        Coupon coupon = couponService.getCouponById(id);
        return ResponseEntity.ok(coupon);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Coupon> updateCoupon(
            @PathVariable Long id,
            @RequestBody CouponCreateRequestDto request
    ) {
        Coupon updatedCoupon = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(updatedCoupon);
    }

    @PostMapping("/bulk")
    public ResponseEntity<Void> bulkAction(@RequestBody ecommerce.dto.admin.coupon.CouponBulkRequestDto request) {
        couponService.performBulkAction(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getCouponStats() {
        return ResponseEntity.ok(couponService.getCouponStatistics());
    }

}