package ecommerce.dto.order;

import ecommerce.enums.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    @NotNull
    private String shippingAddress;

    @NotNull
    private String city;

    @NotNull
    private String area;

    @NotNull
    @Pattern(regexp = "(^([+]{1}[8]{2}|0088)?(01){1}[3-9]{1}\\d{8})$", message = "Phone number not valid")
    private String phone;

    @NotNull
    @Email
    private String email;

    @NotNull
    private String name;

    private String orderNote;

    @NotNull
    private PaymentMethod paymentMethod;

    @NotNull
    private List<OrderItemRequest> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemRequest {

        @NotNull
        private Long productId;

        private int quantity;
    }
}