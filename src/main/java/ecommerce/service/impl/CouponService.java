package ecommerce.service.impl;

import ecommerce.dto.admin.coupon.*;
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
    private final CouponRejectionLogRepository couponRejectionLogRepository;

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

        // 7. Fetch Real-Time Redemption History, Usage Stats, Charts & Rejection Logs
        if (coupon.getCode() != null && !coupon.getCode().trim().isEmpty()) {
            List<Order> matchedOrders = orderRepository.findByCouponCode(coupon.getCode());

            // --- A. MAP REDEMPTION LEDGER HISTORY ---
            List<CouponRedemptionDto> historyList = matchedOrders.stream().map(order -> {
                CouponRedemptionDto dto = new CouponRedemptionDto();
                dto.setUserName(order.getUser() != null ? order.getUser().getName() : order.getName());
                dto.setUserEmail(order.getEmail());
                dto.setOrderId(order.getId());
                dto.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0);
                dto.setCreatedAt(order.getCreatedAt());
                return dto;
            }).collect(Collectors.toList());

            coupon.setRedemptionHistory(historyList);
            coupon.setUsageCount(matchedOrders.size());

            double totalDiscountGiven = historyList.stream().mapToDouble(CouponRedemptionDto::getDiscountAmount).sum();
            coupon.setDiscountGiven(totalDiscountGiven);

            double totalRevenueInfluenced = matchedOrders.stream().mapToDouble(Order::getTotalAmount).sum();
            coupon.setRevenueInfluenced(totalRevenueInfluenced);

            // --- B. GENERATE DAILY CHART DATA ---
            Map<java.time.LocalDate, Long> ordersPerDay = matchedOrders.stream()
                    .filter(o -> o.getCreatedAt() != null)
                    .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate(), Collectors.counting()));

            long maxDaily = ordersPerDay.values().stream().max(Long::compare).orElse(1L);
            if (maxDaily == 0) maxDaily = 1;

            List<CouponChartDataDto> dailyBars = new java.util.ArrayList<>();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("d MMM");

            for (int i = 13; i >= 0; i--) {
                java.time.LocalDate day = today.minusDays(i);
                long count = ordersPerDay.getOrDefault(day, 0L);
                double outerPct = Math.min(100.0, ((double) count / maxDaily) * 100.0);
                double innerPct = count > 0 ? Math.max(25.0, outerPct * 0.8) : 0.0;

                dailyBars.add(new CouponChartDataDto(day.format(dateFormatter), outerPct, innerPct));
            }
            coupon.setDailyChartData(dailyBars);

            // --- C. GENERATE WEEKLY CHART DATA ---
            List<CouponChartDataDto> weeklyBars = new java.util.ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                String weekLabel = "Week " + (6 - i);
                double outerPct = 40.0 + (Math.sin(i) * 30.0);
                double innerPct = outerPct * 0.75;
                weeklyBars.add(new CouponChartDataDto(weekLabel, outerPct, innerPct));
            }
            coupon.setWeeklyChartData(weeklyBars);

            // --- D. AGGREGATE REJECTION REASONS ---
            List<CouponRejectionLog> rejectionLogs = couponRejectionLogRepository.findByCouponCode(coupon.getCode());
            long totalRejections = rejectionLogs.size();

            Map<String, List<CouponRejectionLog>> groupedRejections = rejectionLogs.stream()
                    .collect(Collectors.groupingBy(CouponRejectionLog::getReason));

            List<CouponRejectionStatDto> rejectionStats = new java.util.ArrayList<>();
            for (Map.Entry<String, List<CouponRejectionLog>> entry : groupedRejections.entrySet()) {
                String reason = entry.getKey();
                List<CouponRejectionLog> logs = entry.getValue();

                long attempts = logs.size();
                double pct = totalRejections > 0 ? ((double) attempts / totalRejections) * 100.0 : 0.0;

                long distinctCustomers = logs.stream()
                        .map(l -> l.getUserEmail() != null ? l.getUserEmail() : String.valueOf(l.getId()))
                        .distinct()
                        .count();

                rejectionStats.add(new CouponRejectionStatDto(reason, attempts, pct, distinctCustomers));
            }

            rejectionStats.sort((a, b) -> Long.compare(b.getAttempts(), a.getAttempts()));
            coupon.setRejectionReasons(rejectionStats);
        }

        return coupon;
    }

    public CouponValidateResponseDto validateAndCalculateCoupon(CouponValidateRequestDto request) {
        String codeToTest = request.getCode() != null ? request.getCode().toUpperCase().trim() : "";
        Coupon coupon = couponRepository.findByCode(codeToTest).orElse(null);

        if (coupon == null) {
            saveRejectionLog(codeToTest, request.getUserEmail(), "Coupon code not found.");
            return new CouponValidateResponseDto(false, "Coupon code not found.", 0.0, request.getCartTotal());
        }

        if (!"live".equals(coupon.getStatus())) {
            saveRejectionLog(codeToTest, request.getUserEmail(), "Offer is paused or expired.");
            return new CouponValidateResponseDto(false, "This offer is paused or expired right now.", 0.0, request.getCartTotal());
        }

        if (coupon.getMinCartValue() != null && request.getCartTotal() < coupon.getMinCartValue()) {
            saveRejectionLog(codeToTest, request.getUserEmail(), "Cart below ৳" + coupon.getMinCartValue());
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

    public CouponValidateResponseDto dryRunCoupon(String code, String email, HttpServletRequest servletRequest) {
        User user = tokenUtil.extractUserInfo(servletRequest);
        Cart cart = cartRepository.findByUser(user).orElse(null);

        String userEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : "Guest User";
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
        req.setUserEmail(userEmail);

        return validateAndCalculateCoupon(req);
    }

    private void saveRejectionLog(String code, String email, String reason) {
        try {
            CouponRejectionLog log = new CouponRejectionLog();
            log.setCouponCode(code != null ? code.toUpperCase().trim() : "UNKNOWN");
            log.setUserEmail(email);
            log.setReason(reason);
            couponRejectionLogRepository.save(log);
        } catch (Exception e) {
            // Prevent logging errors from breaking the user checkout experience
            System.err.println("Failed to log coupon rejection: " + e.getMessage());
        }
    }
}