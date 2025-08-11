package ecommerce.service.impl;

import ecommerce.dto.ShippingDto;
import ecommerce.entity.Shipping;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.ShippingRepository;
import ecommerce.service.ShippingService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class ShippingServiceImpl implements ShippingService {

    private final ModelMapper mapper;
    private final ShippingRepository shippingRepository;

    public ShippingServiceImpl(ModelMapper mapper, ShippingRepository shippingRepository) {
        this.mapper = mapper;
        this.shippingRepository = shippingRepository;
    }

    @Override
    public ShippingDto add(ShippingDto shippingDto) {

        Shipping shipping = mapToEntity(shippingDto);
        shippingRepository.save(shipping);
        return mapToDto(shipping);
    }

    @Override
    public ShippingDto update(ShippingDto shippingDto, Long shippingId) {

        Shipping shipping = shippingRepository.findById(shippingId).orElseThrow(()->
                new ResourceNotFound("Shipping", "id", shippingId));

        shipping.setAddress(shippingDto.getAddress());
        shipping.setCity(shippingDto.getCity());
        shipping.setPhone(shippingDto.getPhone());
        shipping.setCountry(shippingDto.getCountry());
        shipping.setPostalCode(shippingDto.getPostalCode());
        shippingRepository.save(shipping);

        return mapToDto(shipping);
    }

    public Shipping mapToEntity(ShippingDto shippingDto) {
        return mapper.map(shippingDto, Shipping.class);
    }

    public ShippingDto mapToDto(Shipping shipping) {
        return mapper.map(shipping, ShippingDto.class);
    }
}
