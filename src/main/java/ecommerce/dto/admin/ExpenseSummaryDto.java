package ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseSummaryDto {
    private List<ExpenseCategoryTotalDto> categoryTotals;
    private double cogsTotal;
    private double operatingTotal;
    private double grandTotal;
}
