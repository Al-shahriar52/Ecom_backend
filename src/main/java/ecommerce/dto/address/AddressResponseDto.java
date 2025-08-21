package ecommerce.dto.address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponseDto {
    private Long id;
    private String addressType;
    private String city;
    private String area;
    private String address;
}
