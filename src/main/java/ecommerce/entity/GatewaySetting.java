package ecommerce.entity;

import ecommerce.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "gateway_settings")
@Data
public class GatewaySetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true)
    private PaymentMethod paymentMethod;

    private double feeRatePct;
    private String settlementCycle;
    private boolean connected = true;
}
