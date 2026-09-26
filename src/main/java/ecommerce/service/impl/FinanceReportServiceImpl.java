package ecommerce.service.impl;

import ecommerce.dto.admin.ReportRowDto;
import ecommerce.repository.ExpenseRepository;
import ecommerce.repository.OrderRepository;
import ecommerce.service.FinanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FinanceReportServiceImpl implements FinanceReportService {

    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;
    private final ecommerce.service.GatewaySettingService gatewaySettingService;

    @Override
    public List<ReportRowDto> getWeeklyOrMonthlyReport(String grain, int periods) {
        List<ReportRowDto> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();

        if ("month".equalsIgnoreCase(grain)) {
            YearMonth currentMonth = YearMonth.now();
            for (int i = 0; i < periods; i++) {
                YearMonth ym = currentMonth.minusMonths(i);
                LocalDate start = ym.atDay(1);
                LocalDate end = ym.atEndOfMonth();
                String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + ym.getYear();
                rows.add(buildRow(label, start, end));
            }
        } else {
            for (int i = 0; i < periods; i++) {
                LocalDate weekStart = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEnd = weekStart.plusDays(6);
                int weekNo = weekStart.get(WeekFields.ISO.weekOfWeekBasedYear());
                String label = "Week " + weekNo + " · " + weekStart.getDayOfMonth() + "–" + weekEnd.getDayOfMonth() + " " +
                        weekEnd.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                rows.add(buildRow(label, weekStart, weekEnd));
            }
        }
        return rows;
    }

    private ReportRowDto buildRow(String label, LocalDate start, LocalDate end) {
        java.time.LocalDateTime startDT = start.atStartOfDay();
        java.time.LocalDateTime endDT = end.plusDays(1).atStartOfDay();

        double gross = orderRepository.sumGrossSales(startDT, endDT);
        double refunds = orderRepository.sumRefunds(startDT, endDT);
        long orders = orderRepository.countPaidOrders(startDT, endDT);

        List<Object[]> mixRows = orderRepository.gatewayMix(startDT, endDT);
        double fees = 0;
        for (Object[] row : mixRows) {
            String method = row[0] != null ? row[0].toString() : "COD";
            double amount = ((Number) row[1]).doubleValue();
            fees += amount * gatewaySettingService.getRate(method) / 100.0;
        }

        double cogs = expenseRepository.sumCogs(start, end);
        double operating = expenseRepository.sumOperating(start, end);
        double net = gross - refunds - fees - cogs - operating;

        return new ReportRowDto(label, orders, gross, refunds, fees, cogs, operating, net);
    }
}
