package ecommerce.service.impl;

import ecommerce.dto.admin.ExpenseCategoryTotalDto;
import ecommerce.dto.admin.PLLineDto;
import ecommerce.dto.admin.ProfitLossDto;
import ecommerce.dto.admin.TakaBreakdownItemDto;
import ecommerce.repository.ExpenseRepository;
import ecommerce.repository.OrderRepository;
import ecommerce.service.FinancePLService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FinancePLServiceImpl implements FinancePLService {

    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;
    private final ecommerce.service.GatewaySettingService gatewaySettingService;

    private static final LinkedHashMap<String, String> LINE_LABELS = new LinkedHashMap<>();
    static {
        LINE_LABELS.put("ADVERTISING", "Advertising");
        LINE_LABELS.put("COURIER_DELIVERY", "Courier & delivery");
        LINE_LABELS.put("PACKAGING", "Packaging");
        LINE_LABELS.put("SALARIES", "Salaries");
        LINE_LABELS.put("RENT_UTILITIES", "Rent & utilities");
        LINE_LABELS.put("SOFTWARE", "Software & subscriptions");
        LINE_LABELS.put("OTHER", "Other");
    }

    private static class Period {
        double grossSales, refunds, netSales, cogs, grossProfit, gatewayFees, operatingTotal, netProfit;
        Map<String, Double> categoryTotals = new HashMap<>();
    }

    private Period compute(LocalDate start, LocalDate end) {
        Period p = new Period();
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.plusDays(1).atStartOfDay();

        p.grossSales = orderRepository.sumGrossSales(startDT, endDT);
        p.refunds = orderRepository.sumRefunds(startDT, endDT);
        p.netSales = p.grossSales - p.refunds;

        List<Object[]> mixRows = orderRepository.gatewayMix(startDT, endDT);
        double fees = 0;
        for (Object[] row : mixRows) {
            String method = row[0] != null ? row[0].toString() : "COD";
            double amount = ((Number) row[1]).doubleValue();
            fees += amount * gatewaySettingService.getRate(method) / 100.0;
        }
        p.gatewayFees = fees;

        List<ExpenseCategoryTotalDto> categoryTotals = expenseRepository.categoryTotals(start, end);
        for (ExpenseCategoryTotalDto c : categoryTotals) {
            p.categoryTotals.put(c.getCategory(), c.getTotal());
        }
        p.cogs = p.categoryTotals.getOrDefault("STOCK_PURCHASE", 0.0);
        p.grossProfit = p.netSales - p.cogs;

        double operating = p.gatewayFees;
        for (String key : LINE_LABELS.keySet()) {
            operating += p.categoryTotals.getOrDefault(key, 0.0);
        }
        p.operatingTotal = operating;
        p.netProfit = p.grossProfit - p.operatingTotal;

        return p;
    }

    @Override
    public ProfitLossDto getProfitLoss(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            LocalDate now = LocalDate.now();
            start = now.withDayOfMonth(1);
            end = now.withDayOfMonth(now.lengthOfMonth());
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate prevEnd = start.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(days - 1);

        Period cur = compute(start, end);
        Period prev = compute(prevStart, prevEnd);

        List<PLLineDto> lines = new ArrayList<>();
        double sales = cur.grossSales > 0 ? cur.grossSales : 1;

        lines.add(new PLLineDto("Gross sales", cur.grossSales, prev.grossSales, 100.0, true, 0));
        lines.add(new PLLineDto("Less returns & refunds", -cur.refunds, -prev.refunds, (cur.refunds / sales) * 100, false, 0));
        lines.add(new PLLineDto("Net sales", cur.netSales, prev.netSales, (cur.netSales / sales) * 100, true, 1));
        lines.add(new PLLineDto("Cost of goods sold", -cur.cogs, -prev.cogs, (cur.cogs / sales) * 100, false, 0));
        lines.add(new PLLineDto("Gross profit", cur.grossProfit, prev.grossProfit, (cur.grossProfit / sales) * 100, true, 1));

        for (Map.Entry<String, String> e : LINE_LABELS.entrySet()) {
            double curVal = cur.categoryTotals.getOrDefault(e.getKey(), 0.0);
            double prevVal = prev.categoryTotals.getOrDefault(e.getKey(), 0.0);
            if (curVal == 0 && prevVal == 0) continue;
            lines.add(new PLLineDto(e.getValue(), -curVal, -prevVal, (curVal / sales) * 100, false, 0));
        }

        lines.add(new PLLineDto("Gateway fees", -cur.gatewayFees, -prev.gatewayFees, (cur.gatewayFees / sales) * 100, false, 0));
        lines.add(new PLLineDto("Total operating cost", -cur.operatingTotal, -prev.operatingTotal, (cur.operatingTotal / sales) * 100, true, 1));
        lines.add(new PLLineDto("Net profit", cur.netProfit, prev.netProfit, (cur.netProfit / sales) * 100, true, 2));

        List<TakaBreakdownItemDto> breakdown = new ArrayList<>();
        breakdown.add(new TakaBreakdownItemDto("Cost of goods", (cur.cogs / sales) * 100));
        for (Map.Entry<String, String> e : LINE_LABELS.entrySet()) {
            double v = cur.categoryTotals.getOrDefault(e.getKey(), 0.0);
            if (v == 0) continue;
            breakdown.add(new TakaBreakdownItemDto(e.getValue(), (v / sales) * 100));
        }
        breakdown.add(new TakaBreakdownItemDto("Refunds", (cur.refunds / sales) * 100));
        breakdown.add(new TakaBreakdownItemDto("Gateway fees", (cur.gatewayFees / sales) * 100));
        breakdown.add(new TakaBreakdownItemDto("Kept as profit", (cur.netProfit / sales) * 100));

        return new ProfitLossDto(lines, breakdown);
    }
}
