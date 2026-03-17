package ecommerce.dto.admin.steadfast.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteadfastBulkResponse {
    private List<ConsignmentData> data;
}