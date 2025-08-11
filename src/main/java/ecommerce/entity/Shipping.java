package ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Shipping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String address;

    @NotNull
    private String city;

    @NotNull
    private String postalCode;

    @NotNull
    private String country;

    @NotNull
    @Pattern(regexp = "(^([+]{1}[8]{2}|0088)?(01){1}[3-9]{1}\\d{8})$",
            message = "Phone number not valid")
    private String phone;

    @NotNull
    @Min(value = 0)
    private double shippingPrice;
}
