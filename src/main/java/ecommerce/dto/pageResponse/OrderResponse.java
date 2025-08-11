package ecommerce.dto.pageResponse;

import ecommerce.dto.OrderDto;
import lombok.Data;

import java.util.List;

@Data
public class OrderResponse {

    private List<OrderDto> content;
    private int pageNo;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean last;
}
