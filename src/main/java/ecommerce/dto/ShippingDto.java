package ecommerce.dto;

import lombok.Data;

@Data
public class ShippingDto {

    private Long id;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private String phone;
    private double shippingPrice;
}
