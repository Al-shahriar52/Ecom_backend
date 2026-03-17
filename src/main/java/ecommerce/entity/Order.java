package ecommerce.entity;

import ecommerce.enums.OrderStatus;
import ecommerce.enums.PaymentMethod;
import ecommerce.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    // --- Contact ---
    private String shippingAddress;
    private String city;
    private String area;

    @Column(name = "phone_number")
    private String phoneNumber;
    private String email;
    private String name;
    private String orderNote;

    // --- Payment ---
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDateTime paidAt;

    // --- Financials ---
    private Double shippingCost;
    private Double totalAmount;

    // --- Status & Timeline ---
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime packagedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    private boolean isDelivered = false;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Invoice invoice;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Delivery delivery;
}