package ecommerce.service;

import ecommerce.dto.ShippingDto;

public interface ShippingService {

    ShippingDto add(ShippingDto shippingDto);
    ShippingDto update(ShippingDto shippingDto, Long shippingId);
}
