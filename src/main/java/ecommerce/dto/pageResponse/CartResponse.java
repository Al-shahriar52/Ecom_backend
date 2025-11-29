package ecommerce.dto.pageResponse;

import lombok.Data;

import java.util.List;


@Data
public class CartResponse {

    private List<Object> content;
    private int pageNo;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean last;
}
