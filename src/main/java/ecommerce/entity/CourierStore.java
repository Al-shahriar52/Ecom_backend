package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** A courier-side pickup store (Pathao). Synced from the courier; the default one is used when booking. */
@Entity
@Data
@Table(name = "courier_stores",
        uniqueConstraints = @UniqueConstraint(name = "uk_courier_store", columnNames = {"provider", "external_store_id"}))
public class CourierStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "external_store_id", nullable = false)
    private Long externalStoreId;

    private String name;

    @Column(length = 255)
    private String address;

    private Long cityId;
    private Long zoneId;
    private Long hubId;

    /** Courier says the store is usable (new stores wait about an hour for approval). */
    private boolean active = true;

    /** Our own default for booking. */
    private boolean defaultStore = false;

    private LocalDateTime syncedAt = LocalDateTime.now();
}
