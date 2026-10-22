package ecommerce.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Everything the tracking screens (customer and admin) need, with the time of each step where we know it. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingDto {
    private Long orderId;
    private String invoice;
    private String orderStatus;
    private String deliveryStatus;
    private String headline;          // e.g. "On its way", "Delivered"
    private String tone;              // progress | success | danger | neutral
    private int progressPct;
    private LocalDateTime placedAt;
    private LocalDateTime finishedAt; // delivered / returned time when finished
    private Long hoursInJourney;      // placed -> finished (or now)
    private String courier;
    private String consignmentId;
    private String trackingCode;
    private String trackingLink;
    private Double codAmount;         // what the rider collects
    private String paymentMethod;
    private String recipientName;
    private String recipientAddress;
    private List<Step> steps;
    private List<Event> courierEvents;     // the courier's own timestamped log (Steadfast)
    private List<Event> statusHistory;     // statuses we recorded, with the time we recorded them
    private String riderName;              // only if the courier told us
    private String riderPhone;
    // admin-only (null for customers)
    private String courierStatus;
    private LocalDateTime lastSyncedAt;
    private Double deliveryFee;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private String key;
        private String title;
        private String detail;
        private String state;          // done | current | upcoming | failed
        private LocalDateTime time;    // null when we have no record of when
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Event {
        private String text;
        private String time;           // ISO time (courier's UTC time for courierEvents)
        private Integer type;          // courier step number when given
    }
}
