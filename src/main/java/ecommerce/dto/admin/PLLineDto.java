package ecommerce.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PLLineDto {
    private String label;
    private double current;
    private double previous;
    private double pctOfSales;
    private boolean bold;
    /** 0 = normal row, 1 = subtotal row, 2 = final (net profit) row */
    private int highlight;
}
