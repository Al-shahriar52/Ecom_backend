package ecommerce.dto.pageResponse;

import ecommerce.dto.WishListDto;
import lombok.Data;

import java.util.List;

@Data
public class WishListResponse {

    private List<WishListDto> items;
}
