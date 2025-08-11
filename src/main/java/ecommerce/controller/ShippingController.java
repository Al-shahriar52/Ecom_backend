package ecommerce.controller;

import ecommerce.dto.ShippingDto;
import org.springframework.http.ResponseEntity;

public interface ShippingController {

    ResponseEntity<?> add(ShippingDto shippingDto);
    ResponseEntity<?> update(ShippingDto shippingDto, Long shippingId);
}
