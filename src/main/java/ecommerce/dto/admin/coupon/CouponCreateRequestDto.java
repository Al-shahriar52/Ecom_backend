package ecommerce.dto.admin.coupon;

import com.fasterxml.jackson.annotation.JsonFormat;
import ecommerce.entity.Coupon;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CouponCreateRequestDto {
    private String code;
    private String internalName;
    private String checkoutMsg;
    private boolean publicCoupon;
    private boolean stackCoupons;
    private Coupon.DiscountType discountType;
    private Double discountValue;
    private Double maxCap;
    private Double minCartValue;
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

    // Multi-selection ID collections
    private List<Long> targetProductIds;
    private List<Long> targetCategoryIds;
    private List<Long> targetSubCategoryIds;
    private List<Long> targetBrandIds;
    private List<Long> targetCityIds;
    private List<Long> targetAreaIds;

    private List<String> channels;
    private List<Coupon.BlackoutPeriod> blackouts;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime endDate;
    private boolean noEndDate;
}