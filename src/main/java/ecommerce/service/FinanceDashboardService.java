package ecommerce.service;

import ecommerce.dto.admin.FinanceDashboardDto;

import java.time.LocalDate;

public interface FinanceDashboardService {
    FinanceDashboardDto getDashboard(LocalDate start, LocalDate end, String grain);
}
