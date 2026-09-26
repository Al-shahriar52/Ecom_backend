package ecommerce.service;

import ecommerce.dto.admin.ReportRowDto;

import java.util.List;

public interface FinanceReportService {
    List<ReportRowDto> getWeeklyOrMonthlyReport(String grain, int periods);
}
