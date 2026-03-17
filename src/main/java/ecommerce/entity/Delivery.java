package ecommerce.entity;

import ecommerce.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    // --- Courier Info ---
    private String courierName; // e.g., "Steadfast", "Pathao", "RedX"
    
    // The Tracking / Consignment ID (CID in the frontend)
    @Column(unique = true)
    private String consignmentId; 

    // Used for Steadfast tracking API if they provide an internal tracking code
    private String trackingCode; 

    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    // --- Additional Logistics Data ---
    private String deliveryType; // e.g., "Home Delivery", "Point/Hub Pickup"
    private Double codAmount;    // How much the courier needs to collect (0 if already paid via Bkash)
    private String deliveryNote;

    // --- Timeline ---
    private LocalDateTime requestedAt; // When you clicked "Pickup" in the Admin Panel
    private LocalDateTime pickedUpAt;  // When the rider actually took it
    private LocalDateTime deliveredAt;
    private LocalDateTime returnedAt;
}