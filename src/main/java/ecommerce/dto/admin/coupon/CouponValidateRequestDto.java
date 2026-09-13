package ecommerce.dto.admin.coupon;

import lombok.Data;

@Data
public class CouponValidateRequestDto {
    private String code;
    private Double cartTotal;
    private String city;
    private String paymentMethod;
    private String userEmail;
}