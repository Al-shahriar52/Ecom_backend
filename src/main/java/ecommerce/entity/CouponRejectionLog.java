package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "coupon_rejection_logs")
public class CouponRejectionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String couponCode;
    private Long userId; // Nullable for guests
    private String userEmail; // To track distinct customers
    
    @Column(columnDefinition = "TEXT")
    private String reason; // e.g., "Cart minimum value of ৳5,000 required."
    
    private LocalDateTime timestamp = LocalDateTime.now();
}