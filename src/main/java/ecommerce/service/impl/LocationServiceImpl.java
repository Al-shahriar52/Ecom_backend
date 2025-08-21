package ecommerce.service.impl;

import ecommerce.dto.location.LocationResponseDto;
import ecommerce.entity.Areas;
import ecommerce.entity.Cities;
import ecommerce.repository.AreasRepository;
import ecommerce.repository.CitiesRepository;
import ecommerce.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final CitiesRepository citiesRepository;
    private final AreasRepository areasRepository;

    @Override
    public List<Cities> cities() {
        return citiesRepository.findAll();
    }

    @Override
    public List<LocationResponseDto> areas(Long cityId) {
        return areasRepository.findAllByCityId(cityId);
    }
}
