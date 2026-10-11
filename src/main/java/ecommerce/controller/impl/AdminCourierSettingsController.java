package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.courier.*;
import ecommerce.service.impl.CourierAdminService;
import ecommerce.service.impl.CourierStatusSyncService;
import ecommerce.service.impl.SteadfastPayoutService;
import ecommerce.service.impl.CustomerCheckService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Provider switches, Pathao stores and Pathao location/quote lookups. */
@RestController
@RequestMapping("/api/v1/admin/courier")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourierSettingsController {

    private final CourierAdminService service;
    private final CourierStatusSyncService syncService;
    private final SteadfastPayoutService payoutService;
    private final CustomerCheckService customerCheckService;
    private final ecommerce.repository.WebhookEventRepository webhookEvents;

    private ResponseEntity<?> ok(String msg, Object data) {
        return new ResponseEntity<>(GenericResponseDto.success(msg, data, HttpStatus.OK.value()), HttpStatus.OK);
    }

    /** Latest verified webhooks, payload shortened. Handy for seeing exactly what Steadfast sends. */
    @GetMapping("/webhook-events")
    public ResponseEntity<?> webhookEvents() {
        var rows = webhookEvents.findTop30ByOrderByReceivedAtDesc().stream().map(e -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("provider", e.getProvider());
            m.put("eventType", e.getEventType());
            m.put("consignmentId", e.getConsignmentId());
            m.put("outcome", e.getOutcome());
            m.put("receivedAt", e.getReceivedAt());
            String p = e.getPayload();
            m.put("payload", p != null && p.length() > 600 ? p.substring(0, 600) + "…" : p);
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ok("Webhook events fetched", rows);
    }

    @GetMapping("/steadfast/customer-check")
    public ResponseEntity<?> customerCheck(@RequestParam String phone) {
        return ok("Customer checked", customerCheckService.check(phone));
    }

    @GetMapping("/steadfast/payouts")
    public ResponseEntity<?> steadfastPayouts(@RequestParam(defaultValue = "1") int page) {
        return ok("Steadfast payouts fetched", payoutService.overview(page));
    }

    @GetMapping("/providers")
    public ResponseEntity<?> providers() { return ok("Couriers fetched", service.listProviders()); }

    @PutMapping("/providers/{key}")
    public ResponseEntity<?> updateProvider(@PathVariable String key, @RequestBody UpdateProviderRequest req) {
        return ok("Courier updated", service.updateProvider(key, req));
    }

    @PostMapping("/providers/pathao/test")
    public ResponseEntity<?> testPathao() {
        service.testPathao();
        return ok("Connected to Pathao", null);
    }

    @GetMapping("/pathao/stores")
    public ResponseEntity<?> stores() { return ok("Stores fetched", service.listPathaoStores()); }

    @PostMapping("/pathao/stores/sync")
    public ResponseEntity<?> syncStores() { return ok("Stores synced", service.syncPathaoStores()); }

    @PostMapping("/pathao/stores")
    public ResponseEntity<?> createStore(@Valid @RequestBody CreatePathaoStoreRequest req) {
        return ok("Store submitted to Pathao. Approval takes about an hour.", service.createPathaoStore(req));
    }

    @PutMapping("/pathao/stores/{id}/default")
    public ResponseEntity<?> defaultStore(@PathVariable Long id) { return ok("Default store set", service.setDefaultPathaoStore(id)); }

    @GetMapping("/pathao/cities")
    public ResponseEntity<?> cities() { return ok("Cities fetched", service.pathaoCities()); }

    @GetMapping("/pathao/cities/{cityId}/zones")
    public ResponseEntity<?> zones(@PathVariable long cityId) { return ok("Zones fetched", service.pathaoZones(cityId)); }

    @GetMapping("/pathao/zones/{zoneId}/areas")
    public ResponseEntity<?> areas(@PathVariable long zoneId) { return ok("Areas fetched", service.pathaoAreas(zoneId)); }

    @PostMapping("/pathao/quote")
    public ResponseEntity<?> quote(@Valid @RequestBody PathaoQuoteRequest req) { return ok("Quote calculated", service.pathaoQuote(req)); }

    /** Ask every courier about parcels that are still moving. Also runs automatically every few minutes. */
    @PostMapping("/sync")
    public ResponseEntity<?> syncAll() { return ok("Sync finished", syncService.syncActive()); }

    @PostMapping("/sync/order/{orderId}")
    public ResponseEntity<?> syncOrder(@PathVariable Long orderId) { return ok("Order synced", syncService.syncOrder(orderId)); }
}
