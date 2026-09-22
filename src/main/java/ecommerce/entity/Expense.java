package ecommerce.entity;

import ecommerce.enums.ExpenseCategory;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Data
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ExpenseCategory category;

    private String vendor;
    private String reference;

    /** Free text describing where the money came from, e.g. "City Bank ····8841" */
    private String paidFrom;

    private double amount;
    private double vatAmount;
    private double aitAmount;

    @Column(length = 1000)
    private String note;

    private boolean recurring;

    /** True = counted as Cost of Goods Sold, false = operating cost */
    private boolean cogs;

    private String receiptUrl;

    private LocalDate expenseDate;

    @ManyToOne
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
