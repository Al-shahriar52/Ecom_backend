package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.courier.CourierProvider;
import ecommerce.courier.CourierRegistry;
import ecommerce.courier.PickupOutcome;
import ecommerce.courier.PickupSpec;
import ecommerce.dto.courier.*;
import ecommerce.entity.PickupLocation;
import ecommerce.entity.PickupRequest;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.DeliveryRepository;
import ecommerce.repository.PickupLocationRepository;
import ecommerce.repository.PickupRequestRepository;
import ecommerce.service.CourierPickupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CourierPickupServiceImpl implements CourierPickupService {

    // Steadfast only accepts 013-019 prefixed numbers for pickup contacts.
    private static final Pattern PICKUP_PHONE = Pattern.compile("^01[3-9]\\d{8}$");

    private final PickupLocationRepository locationRepository;
    private final PickupRequestRepository requestRepository;
    private final DeliveryRepository deliveryRepository;
    private final SteadfastService steadfastService; // police-station lookup is Steadfast-specific data
    private final CourierRegistry courierRegistry;

    @Override
    public List<PickupLocation> getLocations() {
        return locationRepository.findByActiveTrueOrderByPrimaryLocationDescLabelAsc();
    }

    @Override
    @Transactional
    public PickupLocation createLocation(PickupLocationRequest request) {
        PickupLocation loc = new PickupLocation();
        apply(loc, request);
        // The very first location becomes the default automatically.
        if (locationRepository.findByActiveTrueOrderByPrimaryLocationDescLabelAsc().isEmpty()) {
            loc.setPrimaryLocation(true);
        }
        PickupLocation saved = locationRepository.save(loc);
        if (saved.isPrimaryLocation()) clearOtherDefaults(saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public PickupLocation updateLocation(Long id, PickupLocationRequest request) {
        PickupLocation loc = locationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new BadRequestException("Pickup location not found"));
        apply(loc, request);
        PickupLocation saved = locationRepository.save(loc);
        if (saved.isPrimaryLocation()) clearOtherDefaults(saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteLocation(Long id) {
        PickupLocation loc = locationRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new BadRequestException("Pickup location not found"));
        // Soft delete: past requests keep pointing at it.
        loc.setActive(false);
        loc.setPrimaryLocation(false);
        locationRepository.save(loc);
    }

    @Override
    public JsonNode getPoliceStations() {
        return steadfastService.getPoliceStations();
    }

    @Override
    public PickupRequestResult createPickupRequest(CreatePickupRequestDto dto, String createdBy) {
        PickupLocation loc = locationRepository.findByIdAndActiveTrue(dto.getLocationId())
                .orElseThrow(() -> new BadRequestException("Pickup location not found"));

        PickupRequest record = new PickupRequest();
        record.setLocation(loc);
        record.setLocationLabel(loc.getLabel());
        record.setAddress(loc.getAddress());
        record.setContactNumber(loc.getContactNumber());
        record.setNote(dto.getNote());
        record.setEstimQty(dto.getEstimQty());
        record.setCreatedBy(createdBy);

        // Pickup requests are a Steadfast capability; other couriers pick up automatically from the store.
        CourierProvider provider = courierRegistry.resolve("steadfast");
        if (!provider.capabilities().pickupRequest()) {
            throw new BadRequestException(provider.displayName() + " does not use pickup requests");
        }
        PickupOutcome outcome = provider.requestPickup(new PickupSpec(
                loc.getSteadfastAddressId(), loc.getPoliceStationId(), loc.getAddress(),
                loc.getContactNumber(), dto.getNote(), dto.getEstimQty()));

        record.setStatus(outcome.status());
        record.setSteadfastPickupId(outcome.pickupId());
        record.setCourierMessage(truncate(outcome.message(), 1000));
        PickupRequest saved = requestRepository.save(record);

        return PickupRequestResult.builder()
                .id(saved.getId()).status(saved.getStatus()).message(outcome.message()).build();
    }

    @Override
    public Page<PickupRequest> getHistory(int pageNo, int pageSize) {
        return requestRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(pageNo, Math.min(Math.max(pageSize, 1), 100)));
    }

    @Override
    public List<AwaitingPickupDto> getAwaitingPickup() {
        return deliveryRepository.findAwaitingPickup();
    }

    // ---- helpers ----

    private void apply(PickupLocation loc, PickupLocationRequest r) {
        if (!PICKUP_PHONE.matcher(r.getContactNumber().trim()).matches()) {
            throw new BadRequestException("Contact number must be 11 digits and start with 013-019");
        }
        loc.setLabel(r.getLabel().trim());
        loc.setSteadfastAddressId(r.getSteadfastAddressId());
        loc.setPoliceStationId(r.getPoliceStationId());
        loc.setPoliceStationName(r.getPoliceStationName());
        loc.setAddress(r.getAddress().trim());
        loc.setContactNumber(r.getContactNumber().trim());
        loc.setPrimaryLocation(r.isPrimaryLocation());
    }

    private void clearOtherDefaults(Long keepId) {
        for (PickupLocation other : locationRepository.findByActiveTrueOrderByPrimaryLocationDescLabelAsc()) {
            if (!other.getId().equals(keepId) && other.isPrimaryLocation()) {
                other.setPrimaryLocation(false);
                locationRepository.save(other);
            }
        }
    }

    private String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
