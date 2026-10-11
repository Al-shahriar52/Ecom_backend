package ecommerce.dto.courier;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SyncSummaryDto {
    private int checked;
    private int updated;
    private int failed;
    private String message;
}
