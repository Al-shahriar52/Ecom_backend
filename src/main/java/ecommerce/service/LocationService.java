package ecommerce.service;

import ecommerce.dto.location.LocationResponseDto;
import ecommerce.entity.Cities;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface LocationService {

    List<Cities> cities();
    List<LocationResponseDto> areas(Long cityId);
}
