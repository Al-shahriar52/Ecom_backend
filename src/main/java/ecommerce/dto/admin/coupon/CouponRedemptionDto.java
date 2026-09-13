package ecommerce.dto.admin.coupon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponRedemptionDto {
    private String userName;
    private String userEmail;
    private Long orderId;
    private Double discountAmount;
    private LocalDateTime createdAt;
}