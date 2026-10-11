package ecommerce.controller;

import ecommerce.dto.courier.CreatePickupRequestDto;
import ecommerce.dto.courier.PickupLocationRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface AdminCourierController {
    ResponseEntity<?> getLocations();
    ResponseEntity<?> createLocation(PickupLocationRequest request);
    ResponseEntity<?> updateLocation(Long id, PickupLocationRequest request);
    ResponseEntity<?> deleteLocation(Long id);
    ResponseEntity<?> getPoliceStations();
    ResponseEntity<?> createPickupRequest(CreatePickupRequestDto request, HttpServletRequest httpRequest);
    ResponseEntity<?> getHistory(int pageNo, int pageSize);
    ResponseEntity<?> getAwaitingPickup();
}
