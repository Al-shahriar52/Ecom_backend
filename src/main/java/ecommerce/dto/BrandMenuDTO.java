package ecommerce.dto;// Create this new file

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BrandMenuDTO {
    private Long id;
    private String name;
    private String slug;
    private Long productCount;
    private boolean isTop;
    private String logoUrl;
}