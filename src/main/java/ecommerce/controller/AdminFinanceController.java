package ecommerce.controller;

import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

public interface AdminFinanceController {
    ResponseEntity<?> getDashboard(LocalDate start, LocalDate end, String grain);
    ResponseEntity<?> getCodSummary();
    ResponseEntity<?> getProfitLoss(LocalDate start, LocalDate end);
    ResponseEntity<?> getReport(String grain, int periods);
    ResponseEntity<?> getGateways();
    ResponseEntity<?> updateGatewayRate(String paymentMethod, ecommerce.dto.admin.UpdateGatewayRateRequest request);
}