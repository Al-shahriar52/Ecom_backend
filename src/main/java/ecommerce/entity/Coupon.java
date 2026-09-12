package ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "internal_name", nullable = false)
    private String internalName;

    @Column(name = "checkout_msg", length = 500)
    private String checkoutMsg;

    @Column(nullable = false)
    private boolean publicCoupon = true;

    @Column(nullable = false)
    private boolean stackCoupons = false;

    // Discount Details
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType; // PERCENT, FIXED, DELIVERY, PRICE

    private Double discountValue;
    private Double maxCap;
    private Double minCartValue;

    // Eligibility & Limits
    private String eligibility;
    private boolean autoIssue;
    private Integer totalRedemptions;
    private Integer perCustomerLimit;
    private Integer perDayLimit;

    private String targetDomain;
    private Long targetUserId;
    private String targetEmail;
    private String targetPhone;
    private String targetRole;

    @Column(name = "status", nullable = false)
    private String status; // live, sched, paused, draft, expired

    @Column(name = "status_label")
    private String statusLabel; // Running, Scheduled, Paused, Draft, Expired

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Schedule & Validity
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean noEndDate;

    // Multi-selection Target Collections
    @ElementCollection
    @CollectionTable(name = "coupon_target_products", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "product_id")
    private List<Long> targetProductIds;

    @ElementCollection
    @CollectionTable(name = "coupon_target_categories", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "category_id")
    private List<Long> targetCategoryIds;

    @ElementCollection
    @CollectionTable(name = "coupon_target_sub_categories", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "sub_category_id")
    private List<Long> targetSubCategoryIds;

    @ElementCollection
    @CollectionTable(name = "coupon_target_brands", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "brand_id")
    private List<Long> targetBrandIds;

    @ElementCollection
    @CollectionTable(name = "coupon_target_cities", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "city_id")
    private List<Long> targetCityIds;

    @ElementCollection
    @CollectionTable(name = "coupon_target_areas", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "area_id")
    private List<Long> targetAreaIds;

    @ElementCollection
    @CollectionTable(name = "coupon_channels", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "channel_name")
    private List<String> channels;

    @ElementCollection
    @CollectionTable(name = "coupon_blackouts", joinColumns = @JoinColumn(name = "coupon_id"))
    private List<BlackoutPeriod> blackouts;

    @Transient
    private List<String> targetProductNames;

    @Transient
    private List<String> targetCategoryNames;

    @Transient
    private List<String> targetSubCategoryNames;

    @Transient
    private List<String> targetBrandNames;

    @Transient
    private List<String> targetCityNames;

    @Transient
    private List<String> targetAreaNames;

    public enum DiscountType {
        PERCENT, FIXED, DELIVERY, PRICE
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlackoutPeriod {
        private String name;
        private String start;
        private String end;
    }
}