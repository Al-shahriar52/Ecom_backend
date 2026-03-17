package ecommerce.dto.admin;

import lombok.Data;
import java.util.List;

@Data
public class CancelOrderRequest {
    private List<String> invoices;
}