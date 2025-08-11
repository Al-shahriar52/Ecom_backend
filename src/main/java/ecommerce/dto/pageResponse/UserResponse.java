package ecommerce.dto.pageResponse;

import ecommerce.dto.UserDto;
import lombok.Data;

import java.util.List;

@Data
public class UserResponse {

    private List<UserDto> content;
    private int pageNo;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean last;
}
