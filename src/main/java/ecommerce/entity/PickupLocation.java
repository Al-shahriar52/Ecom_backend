package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** A saved pickup address (warehouse / office) registered with Steadfast. */
@Entity
@Data
@Table(name = "pickup_locations")
public class PickupLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Friendly name shown in the admin UI, e.g. "Main warehouse". */
    @Column(nullable = false, length = 80)
    private String label;

    /** Steadfast "address_id" - your merchant address id from the Steadfast panel. */
    @Column(nullable = false)
    private Long steadfastAddressId;

    @Column(nullable = false)
    private Long policeStationId;

    private String policeStationName;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 20)
    private String contactNumber;

    private boolean primaryLocation = false;

    private boolean active = true;

    private LocalDateTime createdAt = LocalDateTime.now();
}
