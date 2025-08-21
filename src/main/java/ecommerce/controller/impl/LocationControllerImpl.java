package ecommerce.controller.impl;

import ecommerce.controller.LocationController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.location.LocationResponseDto;
import ecommerce.entity.Cities;
import ecommerce.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationControllerImpl implements LocationController {

    private final LocationService locationService;

    @Override
    @GetMapping("/cities")
    public ResponseEntity<?> cities() {
        List<Cities> cities = locationService.cities();
        return new ResponseEntity<>(GenericResponseDto.success("Fetch successfully", cities, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @Override
    @GetMapping("/areas")
    public ResponseEntity<?> areas(@RequestParam(name = "city_id") Long cityId) {
        List<LocationResponseDto> areas = locationService.areas(cityId);
        return new ResponseEntity<>(GenericResponseDto.success("Fetch successfully", areas, HttpStatus.OK.value()), HttpStatus.OK);
    }
}
