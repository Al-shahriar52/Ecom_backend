package ecommerce.dto.admin.steadfast.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsignmentData {
    @JsonProperty("invoice")
    private String invoice;

    @JsonProperty("consignment_id")
    private String consignmentId;

    @JsonProperty("tracking_code")
    private String trackingCode;

    private String status;
}