package ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfitLossDto {
    private List<PLLineDto> lines;
    private List<TakaBreakdownItemDto> breakdownOf100;
}
