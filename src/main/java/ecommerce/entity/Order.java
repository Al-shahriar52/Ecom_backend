package ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "customer_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PayMethod paymentMethod;

    @NotNull
    @Min(value = 0)
    private double taxPrice;

    @NotNull
    @Min(value = 0)
    private double shippingPrice;

    @NotNull
    @Min(value = 0)
    private double totalPrice;

    private boolean isPaid;

    private String paidAt;

    private boolean isDelivered;

    private String deliveredAt;

    private String createdAt;

    @OneToMany
    private List<OrderItem> orderItems;

    @ManyToOne
    private User user;

    @OneToOne(cascade = CascadeType.ALL)
    @NotNull
    private Shipping shipping;
}
