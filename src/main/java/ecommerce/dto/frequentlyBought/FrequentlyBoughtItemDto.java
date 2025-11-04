package ecommerce.dto.frequentlyBought;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FrequentlyBoughtItemDto {
    private Long id;
    private String name;
    private String imageUrl;
    private double discountedPrice;
}
