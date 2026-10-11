package ecommerce.dto.courier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PickupRequestResult {
    private Long id;
    /** SUBMITTED | ALREADY_PENDING | FAILED */
    private String status;
    private String message;
}
