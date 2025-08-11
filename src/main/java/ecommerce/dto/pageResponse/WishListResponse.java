package ecommerce.dto.pageResponse;

import ecommerce.dto.WishListDto;
import lombok.Data;

import java.util.List;

@Data
public class WishListResponse {

    private List<WishListDto> content;
    private int pageNo;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean last;
}
