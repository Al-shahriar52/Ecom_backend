package ecommerce.service;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.dto.courier.*;
import ecommerce.entity.PickupLocation;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CourierPickupService {
    List<PickupLocation> getLocations();
    PickupLocation createLocation(PickupLocationRequest request);
    PickupLocation updateLocation(Long id, PickupLocationRequest request);
    void deleteLocation(Long id);
    JsonNode getPoliceStations();
    PickupRequestResult createPickupRequest(CreatePickupRequestDto request, String createdBy);
    Page<PickupHistoryDto> getHistory(int pageNo, int pageSize);
    List<AwaitingPickupDto> getAwaitingPickup();
}
