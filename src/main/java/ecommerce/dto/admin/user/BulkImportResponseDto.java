package ecommerce.dto.admin.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class BulkImportResponseDto {
    private int totalProcessed;
    private int successCount;
    private int failureCount;
    private List<String> errors;
}