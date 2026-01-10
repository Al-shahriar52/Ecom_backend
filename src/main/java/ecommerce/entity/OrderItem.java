package ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @ToString.Exclude
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @ToString.Exclude
    private Product product;

    private int quantity;

    // We store the price at the moment of purchase,
    // in case the product price changes later.
    private double price;
}
