package ecommerce.controller;

import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

public interface AdminFinanceController {
    ResponseEntity<?> getDashboard(LocalDate start, LocalDate end, String grain);
}
