package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Per-courier settings row. Steadfast still reads its key/secret from application properties
 * (move them to env vars); credentials/token columns are used by OAuth couriers like Pathao.
 */
@Entity
@Data
@Table(name = "courier_accounts", uniqueConstraints = @UniqueConstraint(columnNames = "provider"))
public class CourierAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lowercase key matching CourierProvider.key(). */
    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, length = 80)
    private String displayName;

    private boolean enabled = true;

    /** Used when an order is booked without choosing a courier. At most one row is true. */
    private boolean defaultProvider = false;

    /** true = courier's sandbox/test environment. */
    private boolean sandbox = false;

    // --- OAuth token store (Pathao) ---
    @ToString.Exclude
    @Column(length = 2000)
    private String accessToken;
    @ToString.Exclude
    @Column(length = 2000)
    private String refreshToken;
    private LocalDateTime tokenExpiresAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
