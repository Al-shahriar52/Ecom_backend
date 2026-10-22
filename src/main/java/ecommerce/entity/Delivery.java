package ecommerce.entity;

import ecommerce.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "deliveries", uniqueConstraints = @UniqueConstraint(name = "uk_delivery_courier_consignment", columnNames = {"courier_name", "consignment_id"}))
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    // --- Courier Info ---
    @Column(name = "courier_name")
    private String courierName; // e.g., "Steadfast", "Pathao", "RedX"
    
    // The Tracking / Consignment ID (CID in the frontend)
    @Column(name = "consignment_id")
    private String consignmentId; 

    // Used for Steadfast tracking API if they provide an internal tracking code
    private String trackingCode; 

    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    // --- Additional Logistics Data ---
    private String deliveryType; // e.g., "Home Delivery", "Point/Hub Pickup"
    private Double codAmount;    // How much the courier needs to collect (0 if already paid via Bkash)
    private String deliveryNote;
    // What the courier charged us for this parcel (Pathao returns it on booking; null when unknown)
    private Double deliveryFee;
    // Filled only if the courier tells us (e.g. in a webhook). Neither courier documents these fields in its API.
    private String riderName;
    private String riderPhone;
    private LocalDateTime riderUpdatedAt;

    // --- Timeline ---
    // Raw status text last reported by the courier (e.g. "in_review", "Picked Up") and when we last asked.
    private String courierStatus;
    private LocalDateTime lastSyncedAt;

    private LocalDateTime requestedAt; // When you clicked "Pickup" in the Admin Panel
    private LocalDateTime pickedUpAt;  // When the rider actually took it
    private LocalDateTime deliveredAt;
    private LocalDateTime returnedAt;
}