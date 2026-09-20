package ecommerce.dto.admin.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkActionRequestDto {
    @NotNull(message = "Action is required")
    private String action; // "activate", "suspend", "delete"

    @NotEmpty(message = "User IDs list cannot be empty")
    private List<Long> userIds;
}