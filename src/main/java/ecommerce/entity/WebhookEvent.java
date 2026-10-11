package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** Every verified webhook we receive, kept raw so nothing is lost and retries are recognised. */
@Entity
@Data
@Table(name = "webhook_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_idem", columnNames = {"provider", "idempotency_key"}))
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(length = 80)
    private String eventType;

    @Column(length = 120)
    private String consignmentId;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    /** APPLIED, NO_CHANGE, UNMATCHED, LOGGED_ONLY */
    @Column(length = 30)
    private String outcome;

    private LocalDateTime receivedAt;
}
