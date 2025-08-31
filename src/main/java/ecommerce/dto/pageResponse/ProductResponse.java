package ecommerce.dto.pageResponse;

import ecommerce.dto.ProductDto;
import ecommerce.dto.ProductSearchResponseDto;
import lombok.Data;

import java.util.List;

@Data
public class ProductResponse {

    private List<ProductSearchResponseDto> content;
    private int pageNo;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean last;
}
