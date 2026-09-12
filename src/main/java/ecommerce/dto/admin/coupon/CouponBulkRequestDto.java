package ecommerce.dto.admin.coupon;

import lombok.Data;
import java.util.List;

@Data
public class CouponBulkRequestDto {
    private List<Long> ids;
    private String action; // 'pause', 'resume', 'delete'
}