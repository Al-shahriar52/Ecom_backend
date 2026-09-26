package ecommerce.dto.admin;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;

@Data
public class FinanceDashboardDto {
    // Waterfall
    private double grossSales;
    private double refunds;
    private double gatewayFees;
    private double cogs;
    private double operatingCost;
    private double netProfit;
    private double netMarginPct;

    // Hero side stats
    private long ordersPaid;
    private double averageOrder;

    // Tiles
    private double collectedToday;
    private List<Double> collectedTodayTrend;
    private double awaitingSettlement;
    private long awaitingSettlementCount;
    private double codPending;
    private long codPendingCount;
    private double operatingCostThisPeriod;
    private List<Double> operatingCostTrend;

    // Chart: label -> value, oldest to newest
    private LinkedHashMap<String, Double> revenueSeries;
    private LinkedHashMap<String, Double> costSeries;

    private List<GatewayMixItemDto> gatewayMix;
    private List<ExpenseCategoryTotalDto> costBreakdown;

    private List<AdminTransactionDto> recentTransactions;
    private List<ExpenseDto> recentRecurringExpenses;
}
