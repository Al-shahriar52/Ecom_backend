package ecommerce.dto.admin;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExpenseDto {
    private Long id;
    private String category;
    private String vendor;
    private String reference;
    private String paidFrom;
    private double amount;
    private double vatAmount;
    private double aitAmount;
    private String note;
    private boolean recurring;
    private boolean cogs;
    private String receiptUrl;
    private LocalDate expenseDate;
}
