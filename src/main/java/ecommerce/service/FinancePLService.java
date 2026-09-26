package ecommerce.service;

import ecommerce.dto.admin.ProfitLossDto;

import java.time.LocalDate;

public interface FinancePLService {
    ProfitLossDto getProfitLoss(LocalDate start, LocalDate end);
}
