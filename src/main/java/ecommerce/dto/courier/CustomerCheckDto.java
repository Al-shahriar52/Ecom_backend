package ecommerce.dto.courier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerCheckDto {
    private String phone;
    /** GOOD, CAUTION, RISKY or UNKNOWN. Computed by us from the ratios and reports; Steadfast's own score is disabled. */
    private String verdict;
    private String summary;
    private Integer deliveryRatio;       // whole percent; null = nothing finished yet (not the same as 100)
    private Integer cancellationRatio;
    private String volumeBand;           // none, low, medium, high, very_high
    private int totalReports;
    private Map<String, Integer> reportCategories;
}
