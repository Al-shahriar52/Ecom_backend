package ecommerce.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminTransactionDto {
    private Long orderId;
    private String invoice;
    private LocalDateTime date;
    private String customer;
    private String phone;
    /** Lowercase PaymentMethod name: bkash, nagad, rocket, card, cod */
    private String gateway;
    private double gross;
    private double feeRate;
    private double fee;
    private double net;
    /** settled | await | transit | delivered | refunded | failed | cancelled */
    private String status;
}
