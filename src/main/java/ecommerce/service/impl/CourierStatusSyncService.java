package ecommerce.service.impl;

import ecommerce.courier.CourierProvider;
import ecommerce.courier.CourierRegistry;
import ecommerce.courier.CourierStatus;
import ecommerce.dto.courier.SyncSummaryDto;
import ecommerce.dto.courier.SyncTargetDto;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps delivery statuses in step with the couriers. Steadfast can push changes through its webhook (SteadfastWebhookService) and Pathao has none,
 * so this polls as the safety net: every few minutes it asks about parcels that are still moving, least recently checked first.
 */
@Service
@RequiredArgsConstructor
public class CourierStatusSyncService {

    private final DeliveryRepository deliveryRepository;
    private final CourierRegistry registry;
    private final DeliveryStatusApplier applier;

    @Value("${courier.sync.enabled:true}")
    private boolean enabled;
    @Value("${courier.sync.batch-size:60}")
    private int batchSize;
    /** Parcels booked longer ago than this are left alone. */
    @Value("${courier.sync.max-age-days:60}")
    private int maxAgeDays;
    @Value("${courier.sync.delay-between-calls-ms:150}")
    private long delayMs;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${courier.sync.interval-ms:300000}", initialDelayString = "${courier.sync.initial-delay-ms:60000}")
    public void scheduledSync() {
        if (!enabled) return;
        try {
            syncActive();
        } catch (Exception e) {
            System.err.println("Courier status sync failed: " + e.getMessage());
        }
    }

    public SyncSummaryDto syncActive() {
        if (!running.compareAndSet(false, true)) {
            return new SyncSummaryDto(0, 0, 0, "A sync is already running");
        }
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(maxAgeDays);
            List<SyncTargetDto> targets = deliveryRepository.findSyncTargets(since, PageRequest.of(0, Math.max(1, batchSize)));
            return run(targets);
        } finally {
            running.set(false);
        }
    }

    public SyncSummaryDto syncOrder(Long orderId) {
        SyncTargetDto t = deliveryRepository.findSyncTargetByOrderId(orderId)
                .orElseThrow(() -> new BadRequestException("This order has no courier booking yet"));
        return run(List.of(t));
    }

    private SyncSummaryDto run(List<SyncTargetDto> targets) {
        int checked = 0, updated = 0, failed = 0;
        Map<String, Integer> strikes = new HashMap<>();   // consecutive failures per courier
        Set<String> paused = new HashSet<>();             // couriers we stop calling this run (rate limit / outage)

        for (SyncTargetDto t : targets) {
            Optional<CourierProvider> provider = find(t.getCourierName());
            if (provider.isEmpty() || !provider.get().isConfigured() || !provider.get().capabilities().statusLookup()) continue;
            String key = provider.get().key();
            if (paused.contains(key)) continue;

            try {
                Optional<CourierStatus> st = provider.get().fetchStatus(t.getConsignmentId(), t.getInvoiceNumber());
                checked++;
                strikes.put(key, 0);
                if (st.isPresent() && applier.apply(t.getDeliveryId(), st.get())) updated++;
            } catch (Exception e) {
                failed++;
                int n = strikes.merge(key, 1, Integer::sum);
                System.err.println("Status sync failed for " + t.getCourierName() + " " + t.getConsignmentId() + ": " + e.getMessage());
                if (n >= 3) paused.add(key); // likely rate-limited or down; try again next run
            }
            if (delayMs > 0) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        String msg = paused.isEmpty() ? null : "Paused " + String.join(", ", paused) + " after repeated errors";
        return new SyncSummaryDto(checked, updated, failed, msg);
    }

    private Optional<CourierProvider> find(String courierName) {
        if (courierName == null) return Optional.empty();
        return registry.all().stream().filter(p -> p.displayName().equalsIgnoreCase(courierName)).findFirst();
    }
}
