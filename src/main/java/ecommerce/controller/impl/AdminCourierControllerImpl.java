package ecommerce.controller.impl;

import ecommerce.controller.AdminCourierController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.courier.CreatePickupRequestDto;
import ecommerce.dto.courier.PickupLocationRequest;
import ecommerce.dto.courier.PickupRequestResult;
import ecommerce.entity.User;
import ecommerce.service.CourierPickupService;
import ecommerce.utils.TokenUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/courier")
@RequiredArgsConstructor
public class AdminCourierControllerImpl implements AdminCourierController {

    private final CourierPickupService courierPickupService;
    private final TokenUtil tokenUtil;

    private ResponseEntity<?> ok(String msg, Object data) {
        return new ResponseEntity<>(GenericResponseDto.success(msg, data, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/pickup-locations")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@perm.has(authentication, 15)")
    @Override
    public ResponseEntity<?> getLocations() {
        return ok("Pickup locations fetched", courierPickupService.getLocations());
    }

    @PostMapping("/pickup-locations")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@perm.has(authentication, 15)")
    @Override
    public ResponseEntity<?> createLocation(@Valid @RequestBody PickupLocationRequest request) {
        return ok("Pickup location saved", courierPickupService.createLocation(request));
    }

    @PutMapping("/pickup-locations/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@perm.has(authentication, 15)")
    @Override
    public ResponseEntity<?> updateLocation(@PathVariable Long id, @Valid @RequestBody PickupLocationRequest request) {
        return ok("Pickup location updated", courierPickupService.updateLocation(id, request));
    }

    @DeleteMapping("/pickup-locations/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@perm.has(authentication, 15)")
    @Override
    public ResponseEntity<?> deleteLocation(@PathVariable Long id) {
        courierPickupService.deleteLocation(id);
        return ok("Pickup location removed", null);
    }

    @GetMapping("/police-stations")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@perm.has(authentication, 15)")
    @Override
    public ResponseEntity<?> getPoliceStations() {
        return ok("Police stations fetched", courierPickupService.getPoliceStations());
    }

    @PostMapping("/pickup-requests")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@perm.has(authentication, 15)")
    @Override
    public ResponseEntity<?> createPickupRequest(@Valid @RequestBody CreatePickupRequestDto request, HttpServletRequest httpRequest) {
        User admin = tokenUtil.extractUserInfo(httpRequest);
        PickupRequestResult result = courierPickupService.createPickupRequest(request, admin.getEmail() != null ? admin.getEmail() : admin.getPhone());
        // Always 200 with a status field: ALREADY_PENDING and FAILED are outcomes the UI renders, not transport errors.
        return ok(result.getMessage(), result);
    }

    @GetMapping("/pickup-requests")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@perm.has(authentication, 15)")
    @Override
    public ResponseEntity<?> getHistory(@RequestParam(defaultValue = "0") int pageNo,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        return ok("Pickup history fetched", courierPickupService.getHistory(pageNo, pageSize));
    }

    @GetMapping("/awaiting-pickup")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("@perm.has(authentication, 15)")
    @Override
    public ResponseEntity<?> getAwaitingPickup() {
        return ok("Parcels awaiting pickup fetched", courierPickupService.getAwaitingPickup());
    }
}
