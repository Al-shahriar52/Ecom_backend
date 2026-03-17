package ecommerce.entity;

import ecommerce.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The human-readable ID like "INV-1001"
    @Column(unique = true, nullable = false)
    private String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    // --- Financials ---
    private Double subTotal;
    private Double taxAmount = 0.0;
    private Double discountAmount = 0.0;
    private Double shippingAmount;
    private Double totalAmount; // Grand total

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    // --- Dates ---
    private LocalDateTime issuedAt = LocalDateTime.now();
    private LocalDateTime paidAt;
    private LocalDateTime dueDate;
}