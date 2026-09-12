package ecommerce.service.impl;

import ecommerce.dto.admin.coupon.CouponBulkRequestDto;
import ecommerce.dto.admin.coupon.CouponCreateRequestDto;
import ecommerce.dto.admin.coupon.CouponValidateRequestDto;
import ecommerce.dto.admin.coupon.CouponValidateResponseDto;
import ecommerce.entity.*;
import ecommerce.repository.*;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final CitiesRepository cityRepository;
    private final AreasRepository areaRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final TokenUtil tokenUtil;

    @Transactional
    public Coupon createCoupon(CouponCreateRequestDto request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists: " + request.getCode());
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase().trim())
                .internalName(request.getInternalName())
                .checkoutMsg(request.getCheckoutMsg())
                .publicCoupon(request.isPublicCoupon())
                .stackCoupons(request.isStackCoupons())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxCap(request.getMaxCap())
                .minCartValue(request.getMinCartValue())
                .eligibility(request.getEligibility())
                .autoIssue(request.isAutoIssue())
                .totalRedemptions(request.getTotalRedemptions())
                .perCustomerLimit(request.getPerCustomerLimit())
                .perDayLimit(request.getPerDayLimit())
                .targetDomain(request.getTargetDomain())
                .targetUserId(request.getTargetUserId())
                .targetEmail(request.getTargetEmail())
                .targetPhone(request.getTargetPhone())
                .targetRole(request.getTargetRole())
                .targetProductIds(request.getTargetProductIds())
                .targetCategoryIds(request.getTargetCategoryIds())
                .targetSubCategoryIds(request.getTargetSubCategoryIds())
                .targetBrandIds(request.getTargetBrandIds())
                .targetCityIds(request.getTargetCityIds())
                .targetAreaIds(request.getTargetAreaIds())
                .channels(request.getChannels())
                .blackouts(request.getBlackouts())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .noEndDate(request.isNoEndDate())
                .status("live")
                .statusLabel("Running")
                .build();

        return couponRepository.save(coupon);
    }

    public Coupon getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found with id: " + id));

        // 1. Fetch Product Names
        if (coupon.getTargetProductIds() != null && !coupon.getTargetProductIds().isEmpty()) {
            List<String> names = productRepository.findAllById(coupon.getTargetProductIds())
                    .stream().map(Product::getName).collect(Collectors.toList());
            coupon.setTargetProductNames(names);
        }

        // 2. Fetch Category Names
        if (coupon.getTargetCategoryIds() != null && !coupon.getTargetCategoryIds().isEmpty()) {
            List<String> names = categoryRepository.findAllById(coupon.getTargetCategoryIds())
                    .stream().map(Category::getName).collect(Collectors.toList());
            coupon.setTargetCategoryNames(names);
        }

        // 3. Fetch Sub-Category Names
        if (coupon.getTargetSubCategoryIds() != null && !coupon.getTargetSubCategoryIds().isEmpty()) {
            List<String> names = subCategoryRepository.findAllById(coupon.getTargetSubCategoryIds())
                    .stream().map(SubCategory::getName).collect(Collectors.toList());
            coupon.setTargetSubCategoryNames(names);
        }

        // 4. Fetch Brand Names
        if (coupon.getTargetBrandIds() != null && !coupon.getTargetBrandIds().isEmpty()) {
            List<String> names = brandRepository.findAllById(coupon.getTargetBrandIds())
                    .stream().map(Brand::getName).collect(Collectors.toList());
            coupon.setTargetBrandNames(names);
        }

        // 5. Fetch City Names
        if (coupon.getTargetCityIds() != null && !coupon.getTargetCityIds().isEmpty()) {
            List<String> names = cityRepository.findAllById(coupon.getTargetCityIds())
                    .stream().map(Cities::getName).collect(Collectors.toList());
            coupon.setTargetCityNames(names);
        }

        // 6. Fetch Area Names
        if (coupon.getTargetAreaIds() != null && !coupon.getTargetAreaIds().isEmpty()) {
            List<String> names = areaRepository.findAllById(coupon.getTargetAreaIds())
                    .stream().map(Areas::getName).collect(Collectors.toList());
            coupon.setTargetAreaNames(names);
        }

        return coupon;
    }

    @Transactional
    public void deleteCoupon(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new RuntimeException("Coupon not found with id: " + id);
        }
        couponRepository.deleteById(id);
    }

    @Transactional
    public Coupon updateCoupon(Long id, CouponCreateRequestDto request) {
        Coupon coupon = getCouponById(id);

        coupon.setCode(request.getCode().toUpperCase().trim());
        coupon.setInternalName(request.getInternalName());
        coupon.setCheckoutMsg(request.getCheckoutMsg());
        coupon.setPublicCoupon(request.isPublicCoupon());
        coupon.setStackCoupons(request.isStackCoupons());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMaxCap(request.getMaxCap());
        coupon.setMinCartValue(request.getMinCartValue());
        coupon.setEligibility(request.getEligibility());
        coupon.setAutoIssue(request.isAutoIssue());
        coupon.setTotalRedemptions(request.getTotalRedemptions());
        coupon.setPerCustomerLimit(request.getPerCustomerLimit());
        coupon.setPerDayLimit(request.getPerDayLimit());
        coupon.setTargetDomain(request.getTargetDomain());
        coupon.setTargetUserId(request.getTargetUserId());
        coupon.setTargetEmail(request.getTargetEmail());
        coupon.setTargetPhone(request.getTargetPhone());
        coupon.setTargetRole(request.getTargetRole());
        coupon.setTargetProductIds(request.getTargetProductIds());
        coupon.setTargetCategoryIds(request.getTargetCategoryIds());
        coupon.setTargetSubCategoryIds(request.getTargetSubCategoryIds());
        coupon.setTargetBrandIds(request.getTargetBrandIds());
        coupon.setTargetCityIds(request.getTargetCityIds());
        coupon.setTargetAreaIds(request.getTargetAreaIds());
        coupon.setChannels(request.getChannels());
        coupon.setBlackouts(request.getBlackouts());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());
        coupon.setNoEndDate(request.isNoEndDate());

        return couponRepository.save(coupon);
    }

    @Transactional
    public void performBulkAction(CouponBulkRequestDto request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return;
        }

        switch (request.getAction().toLowerCase()) {
            case "pause":
                List<Coupon> couponsToPause = couponRepository.findAllById(request.getIds());
                for (Coupon coupon : couponsToPause) {
                    coupon.setStatus("paused");
                    coupon.setStatusLabel("Paused");
                }
                couponRepository.saveAll(couponsToPause);
                break;
            case "resume":
                List<Coupon> couponsToResume = couponRepository.findAllById(request.getIds());
                for (Coupon coupon : couponsToResume) {
                    coupon.setStatus("live");
                    coupon.setStatusLabel("Running");
                }
                couponRepository.saveAll(couponsToResume);
                break;
            case "delete":
                couponRepository.deleteAllById(request.getIds());
                break;
            default:
                throw new IllegalArgumentException("Invalid bulk action: " + request.getAction());
        }
    }

    public CouponValidateResponseDto validateAndCalculateCoupon(CouponValidateRequestDto request) {
        Coupon coupon = couponRepository.findByCode(request.getCode().toUpperCase().trim())
                .orElse(null);

        if (coupon == null) {
            return new CouponValidateResponseDto(false, "Coupon code not found.", 0.0, request.getCartTotal());
        }

        if (!"live".equals(coupon.getStatus())) {
            return new CouponValidateResponseDto(false, "This offer is paused or expired right now.", 0.0, request.getCartTotal());
        }

        if (coupon.getMinCartValue() != null && request.getCartTotal() < coupon.getMinCartValue()) {
            return new CouponValidateResponseDto(false, "Cart minimum value of ৳" + coupon.getMinCartValue() + " required.", 0.0, request.getCartTotal());
        }

        Double discount = 0.0;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
            discount = (request.getCartTotal() * coupon.getDiscountValue()) / 100.0;
            if (coupon.getMaxCap() != null && discount > coupon.getMaxCap()) {
                discount = coupon.getMaxCap();
            }
        } else if (coupon.getDiscountType() == Coupon.DiscountType.FIXED) {
            discount = coupon.getDiscountValue();
        }

        Double finalTotal = Math.max(0.0, request.getCartTotal() - discount);
        return new CouponValidateResponseDto(true, "Coupon applied successfully!", discount, finalTotal);
    }

    public java.util.Map<String, Object> getCouponStatistics() {
        long totalCoupons = couponRepository.count();
        long runningCoupons = couponRepository.countByStatus("live");
        long pausedCoupons = couponRepository.countByStatus("paused");
        long scheduledCoupons = couponRepository.countByStatus("sched");
        Long totalRedemptions = couponRepository.sumTotalRedemptions();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCoupons", totalCoupons);
        stats.put("runningCoupons", runningCoupons);
        stats.put("pausedCoupons", pausedCoupons);
        stats.put("scheduledCoupons", scheduledCoupons);
        stats.put("totalRedemptionsAllowed", totalRedemptions != null ? totalRedemptions : 0);
        return stats;
    }

    public List<Coupon> getAvailableCouponsForUser(HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);
        if (user == null) {
            log.warn("Coupon check failed: User could not be extracted from request token.");
            return Collections.emptyList();
        }

        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart == null) {
            log.info("Coupon check: No cart found for user ID: {}", user.getId());
            return Collections.emptyList();
        }

        List<CartItem> cartItems = cartItemRepository.findByCartAndIsActiveTrue(cart);
        if (cartItems.isEmpty()) {
            log.info("Coupon check: Cart is empty or has no active items for user ID: {}", user.getId());
            return Collections.emptyList();
        }

        double cartTotal = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getDiscountedPrice() * item.getQuantity())
                .sum();

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> liveCoupons = couponRepository.findByStatus("live");
        long pastOrderCount = orderRepository.countByUser(user);

        log.info("Evaluating {} live coupons for User ID: {} | Cart Total: {} | Past Orders: {}",
                liveCoupons.size(), user.getId(), cartTotal, pastOrderCount);

        return liveCoupons.stream().filter(coupon -> {
            String code = coupon.getCode();

            // 1. Date & Schedule Validation
            if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
                System.out.println("DEBUG: Coupon [" + code + "] rejected: Start date is in the future.");
                return false;
            }
            if (!coupon.isNoEndDate() && coupon.getEndDate() != null && now.isAfter(coupon.getEndDate())) {
                System.out.println("DEBUG: Coupon [" + code + "] rejected: Coupon has expired.");
                return false;
            }

            // 2. Minimum Cart Value
            if (coupon.getMinCartValue() != null && cartTotal < coupon.getMinCartValue()) {
                System.out.println("DEBUG: Coupon [" + code + "] rejected: Cart total (" + cartTotal + ") is less than min required (" + coupon.getMinCartValue() + ").");
                return false;
            }

            // 3. Eligibility Tier (User has 9 past orders based on your log)
            String eligibility = coupon.getEligibility();
            if ("First-time customers only".equalsIgnoreCase(eligibility) && pastOrderCount > 0) {
                System.out.println("DEBUG: Coupon [" + code + "] rejected: Requires first-time customer, but user has " + pastOrderCount + " past orders.");
                return false;
            }
            if ("Returning customers only".equalsIgnoreCase(eligibility) && pastOrderCount == 0) {
                System.out.println("DEBUG: Coupon [" + code + "] rejected: Requires returning customer, but user has 0 past orders.");
                return false;
            }

            // 4. Specific User / Email / Phone / Domain targeting
            if (coupon.getTargetUserId() != null && !coupon.getTargetUserId().equals(user.getId())) {
                System.out.println("DEBUG: Coupon [" + code + "] rejected: Target user ID mismatch.");
                return false;
            }
            if (coupon.getTargetEmail() != null && !coupon.getTargetEmail().trim().isEmpty()) {
                if (user.getEmail() == null || !coupon.getTargetEmail().trim().equalsIgnoreCase(user.getEmail())) {
                    System.out.println("DEBUG: Coupon [" + code + "] rejected: Target email mismatch.");
                    return false;
                }
            }
            if (coupon.getTargetPhone() != null && !coupon.getTargetPhone().trim().isEmpty()) {
                if (user.getPhone() == null || !coupon.getTargetPhone().trim().equals(user.getPhone())) {
                    System.out.println("DEBUG: Coupon [" + code + "] rejected: Target phone mismatch.");
                    return false;
                }
            }
            if (coupon.getTargetDomain() != null && !coupon.getTargetDomain().trim().isEmpty()) {
                if (user.getEmail() == null || !user.getEmail().endsWith("@" + coupon.getTargetDomain().trim())) {
                    System.out.println("DEBUG: Coupon [" + code + "] rejected: Target domain mismatch.");
                    return false;
                }
            }

            // 5. Product / Category / Brand Constraints
            boolean matchesProduct = (coupon.getTargetProductIds() == null || coupon.getTargetProductIds().isEmpty()) ||
                    cartItems.stream().anyMatch(i -> coupon.getTargetProductIds().contains(i.getProduct().getId()));

            boolean matchesCategory = (coupon.getTargetCategoryIds() == null || coupon.getTargetCategoryIds().isEmpty()) ||
                    cartItems.stream().anyMatch(i -> i.getProduct().getCategory() != null && coupon.getTargetCategoryIds().contains(i.getProduct().getCategory().getId()));

            boolean matchesBrand = (coupon.getTargetBrandIds() == null || coupon.getTargetBrandIds().isEmpty()) ||
                    cartItems.stream().anyMatch(i -> i.getProduct().getBrand() != null && coupon.getTargetBrandIds().contains(i.getProduct().getBrand().getId()));

            if (!matchesProduct) {
                System.out.println("DEBUG: Coupon [" + code + "] rejected: Cart items do not match target product IDs.");
                return false;
            }
            if (!matchesCategory) {
                System.out.println("DEBUG: Coupon [" + code + "] rejected: Cart items do not match target category IDs.");
                return false;
            }
            if (!matchesBrand) {
                System.out.println("DEBUG: Coupon [" + code + "] rejected: Cart items do not match target brand IDs.");
                return false;
            }

            System.out.println("DEBUG: Coupon [" + code + "] passed all checks!");
            return true;
        }).collect(Collectors.toList());
    }

    public CouponValidateResponseDto dryRunCoupon(String code, HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);
        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart == null) {
            return new CouponValidateResponseDto(false, "Cart is empty.", 0.0, 0.0);
        }

        List<CartItem> cartItems = cartItemRepository.findByCartAndIsActiveTrue(cart);
        double cartTotal = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getDiscountedPrice() * item.getQuantity())
                .sum();

        CouponValidateRequestDto req = new CouponValidateRequestDto();
        req.setCode(code);
        req.setCartTotal(cartTotal);

        return validateAndCalculateCoupon(req);
    }
}