package ecommerce.service.impl;

import ecommerce.dto.admin.*;
import ecommerce.repository.ExpenseRepository;
import ecommerce.repository.OrderRepository;
import ecommerce.service.AdminOrderService;
import ecommerce.service.FinanceDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceDashboardServiceImpl implements FinanceDashboardService {

    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;
    private final AdminOrderService adminOrderService;
    private final ecommerce.service.ExpenseService expenseService;
    private final ecommerce.service.GatewaySettingService gatewaySettingService;

    @Override
    public FinanceDashboardDto getDashboard(LocalDate start, LocalDate end, String grain) {
        if (start == null || end == null) {
            LocalDate now = LocalDate.now();
            start = now.withDayOfMonth(1);
            end = now.withDayOfMonth(now.lengthOfMonth());
        }
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.plusDays(1).atStartOfDay();

        FinanceDashboardDto dto = new FinanceDashboardDto();

        double grossSales = orderRepository.sumGrossSales(startDT, endDT);
        double refunds = orderRepository.sumRefunds(startDT, endDT);
        long ordersPaid = orderRepository.countPaidOrders(startDT, endDT);

        List<Object[]> mixRows = orderRepository.gatewayMix(startDT, endDT);
        List<GatewayMixItemDto> gatewayMix = new ArrayList<>();
        double gatewayFees = 0;
        for (Object[] row : mixRows) {
            String method = row[0] != null ? row[0].toString() : "COD";
            double amount = ((Number) row[1]).doubleValue();
            long count = ((Number) row[2]).longValue();
            gatewayMix.add(new GatewayMixItemDto(method.toLowerCase(), amount, count));
            gatewayFees += amount * gatewaySettingService.getRate(method) / 100.0;
        }
        gatewayMix.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));

        double cogs = expenseRepository.sumCogs(start, end);
        double operatingCost = expenseRepository.sumOperating(start, end);
        double netProfit = grossSales - refunds - gatewayFees - cogs - operatingCost;

        dto.setGrossSales(grossSales);
        dto.setRefunds(refunds);
        dto.setGatewayFees(gatewayFees);
        dto.setCogs(cogs);
        dto.setOperatingCost(operatingCost);
        dto.setNetProfit(netProfit);
        dto.setNetMarginPct(grossSales > 0 ? (netProfit / grossSales) * 100 : 0);
        dto.setOrdersPaid(ordersPaid);
        dto.setAverageOrder(ordersPaid > 0 ? grossSales / ordersPaid : 0);
        dto.setGatewayMix(gatewayMix);
        dto.setCostBreakdown(expenseRepository.categoryTotals(start, end));
        dto.setOperatingCostThisPeriod(operatingCost);

        // --- "Collected today" tile + last-12-day trend ---
        LocalDate today = LocalDate.now();
        List<Double> collectedTrend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            double v = orderRepository.sumCollected(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
            collectedTrend.add(v);
        }
        dto.setCollectedToday(collectedTrend.get(collectedTrend.size() - 1));
        dto.setCollectedTodayTrend(collectedTrend);

        // --- "Awaiting settlement" (current snapshot, not period-based) ---
        dto.setAwaitingSettlement(orderRepository.sumAwaitingSettlement());
        dto.setAwaitingSettlementCount(orderRepository.countAwaitingSettlement());

        // --- "Cash on delivery pending" (current snapshot) ---
        dto.setCodPending(orderRepository.sumCodPending());
        dto.setCodPendingCount(orderRepository.countCodPending());

        // --- Operating cost trend, last 12 weeks ---
        List<Double> opTrend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            opTrend.add(expenseRepository.sumOperating(weekStart, weekEnd));
        }
        dto.setOperatingCostTrend(opTrend);

        // --- Revenue vs cost chart, last 12 buckets (week or month) ---
        double blendedFeeRate = grossSales > 0 ? gatewayFees / grossSales : 0;
        LinkedHashMap<String, Double> revenueSeries = new LinkedHashMap<>();
        LinkedHashMap<String, Double> costSeries = new LinkedHashMap<>();

        if ("month".equalsIgnoreCase(grain)) {
            java.time.YearMonth currentMonth = java.time.YearMonth.now();
            for (int i = 11; i >= 0; i--) {
                java.time.YearMonth ym = currentMonth.minusMonths(i);
                LocalDate bStart = ym.atDay(1);
                LocalDate bEnd = ym.atEndOfMonth();
                double bGross = orderRepository.sumGrossSales(bStart.atStartOfDay(), bEnd.plusDays(1).atStartOfDay());
                double bRefund = orderRepository.sumRefunds(bStart.atStartOfDay(), bEnd.plusDays(1).atStartOfDay());
                double bExpense = expenseRepository.sumTotal(bStart, bEnd);
                double bFee = bGross * blendedFeeRate;
                String label = ym.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
                revenueSeries.put(label, bGross);
                costSeries.put(label, bRefund + bFee + bExpense);
            }
        } else {
            for (int i = 11; i >= 0; i--) {
                LocalDate weekStart = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEnd = weekStart.plusDays(6);
                double bGross = orderRepository.sumGrossSales(weekStart.atStartOfDay(), weekEnd.plusDays(1).atStartOfDay());
                double bRefund = orderRepository.sumRefunds(weekStart.atStartOfDay(), weekEnd.plusDays(1).atStartOfDay());
                double bExpense = expenseRepository.sumTotal(weekStart, weekEnd);
                double bFee = bGross * blendedFeeRate;
                String label = "W" + weekStart.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
                revenueSeries.put(label, bGross);
                costSeries.put(label, bRefund + bFee + bExpense);
            }
        }
        dto.setRevenueSeries(revenueSeries);
        dto.setCostSeries(costSeries);

        // --- Recent transactions (reuses the real transactions endpoint logic) ---
        Page<AdminTransactionDto> recent = adminOrderService.getFinanceTransactions(
                0, 6, null, "All", "All", "All", "All", null, null);
        dto.setRecentTransactions(recent.getContent());
        dto.setRecentRecurringExpenses(expenseService.recentRecurring());

        return dto;
    }
}