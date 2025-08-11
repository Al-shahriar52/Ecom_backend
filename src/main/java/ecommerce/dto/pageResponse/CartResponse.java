package ecommerce.dto.pageResponse;

import ecommerce.dto.CartDto;
import lombok.Data;

import java.util.List;

@Data
public class CartResponse {

    private List<CartDto> content;
    private int pageNo;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean last;
}
