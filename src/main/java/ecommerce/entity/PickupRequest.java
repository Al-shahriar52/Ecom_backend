package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/** One "send a rider to collect parcels" request made to Steadfast. */
@Entity
@Data
@Table(name = "pickup_requests")
public class PickupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    @ToString.Exclude
    private PickupLocation location;

    // Snapshot so history stays correct if the location is edited later.
    private String locationLabel;
    @Column(length = 255)
    private String address;
    private String contactNumber;
    @Column(length = 500)
    private String note;
    private Integer estimQty;

    /** SUBMITTED | ALREADY_PENDING | FAILED */
    @Column(nullable = false, length = 20)
    private String status;

    /** Id Steadfast returned for the request, when it provides one. */
    private String steadfastPickupId;

    @Column(length = 1000)
    private String courierMessage;

    private String createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
}
