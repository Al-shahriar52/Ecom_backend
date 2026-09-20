package ecommerce.dto.admin.coupon;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CouponValidateResponseDto {
    private boolean valid;
    private String message;
    private Double discountAmount;
    private Double finalTotal;
}