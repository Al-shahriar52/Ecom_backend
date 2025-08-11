package ecommerce.dto.pageResponse;

import ecommerce.dto.ProductDto;
import lombok.Data;

import java.util.List;

@Data
public class ProductResponse {

    private List<ProductDto> content;
    private int pageNo;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean last;
}
