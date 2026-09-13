package ecommerce.dto.admin.coupon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponChartDataDto {
    private String label;
    private double outer; // e.g., Applied/Attempted percentage (0-100)
    private double inner; // e.g., Redeemed percentage (0-100)
}