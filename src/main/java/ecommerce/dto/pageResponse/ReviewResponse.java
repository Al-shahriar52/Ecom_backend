package ecommerce.dto.pageResponse;

import ecommerce.dto.ReviewDto;
import ecommerce.dto.ReviewResponseDto;
import lombok.Data;

import java.util.List;

@Data
public class ReviewResponse {

    private List<ReviewResponseDto> content;
    private int pageNo;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean last;
}
