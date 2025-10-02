package ecommerce.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TagFilterDTO {
    private Long tagId;
    private String tagName;
    private Long productCount;
}