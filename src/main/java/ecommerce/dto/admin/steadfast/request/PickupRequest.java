package ecommerce.dto.admin.steadfast.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PickupRequest {
    @NotNull
    private List<Long> orderIds;

    /** Optional courier key ("steadfast", "pathao"). Omit to use the default courier. */
    private String courier;
}