package ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TakaBreakdownItemDto {
    private String label;
    private double amountPer100;
}
