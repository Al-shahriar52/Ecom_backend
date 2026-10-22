package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** Every time a courier status changed on a delivery, and when we learned of it. Gives each parcel a status history. */
@Entity
@Data
@Table(name = "delivery_events", indexes = @Index(name = "idx_delivery_events_delivery", columnList = "delivery_id"))
public class DeliveryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id", nullable = false)
    private Long deliveryId;

    @Column(length = 100)
    private String rawStatus;       // what the courier said, e.g. "in_review", "Picked Up"

    @Column(length = 30)
    private String mappedStatus;    // our DeliveryStatus, if it maps

    @Column(length = 20)
    private String source;          // WEBHOOK or SYNC

    private LocalDateTime occurredAt; // when we recorded it
}
