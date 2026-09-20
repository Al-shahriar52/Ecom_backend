package ecommerce.dto.admin.coupon;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CouponRejectionStatDto {
    private String reason;
    private long attempts;
    private double pct; // Share percentage (0-100)
    private long customers; // Distinct customers count
}