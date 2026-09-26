package ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportRowDto {
    private String period;
    private long orders;
    private double grossSales;
    private double refunds;
    private double gatewayFees;
    private double cogs;
    private double operatingCost;
    private double netEarning;
}
