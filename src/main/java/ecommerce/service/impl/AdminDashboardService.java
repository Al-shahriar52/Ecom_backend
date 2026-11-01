package ecommerce.service.impl;

import ecommerce.entity.Order;
import ecommerce.enums.OrderStatus;
import ecommerce.repository.AdminDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Numbers for the admin dashboard. Each block is only included when the caller's role has the matching
 * permission in the Roles & Permissions matrix (admin gets everything).
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Dhaka");

    private final AdminDashboardRepository repo;
    private final RoleMatrixService roles;

    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) out.put((String) kv[i], kv[i + 1]);
        return out;
    }

    private static boolean counts(OrderStatus s) { return s != OrderStatus.CANCELLED && s != OrderStatus.RETURNED; }

    public Map<String, Object> build(Collection<String> roleNames, int daysParam) {
        int days = daysParam <= 7 ? 7 : daysParam <= 30 ? 30 : 90;
        boolean canOrders = roles.hasPermission(roleNames, RoleMatrixService.VIEW_ORDERS);
        boolean canProducts = roles.hasPermission(roleNames, RoleMatrixService.MANAGE_PRODUCTS);
        boolean canUsers = roles.hasPermission(roleNames, RoleMatrixService.MANAGE_USERS);
        boolean canMoney = roles.hasPermission(roleNames, RoleMatrixService.MANAGE_ACCOUNTING);
        boolean canCourier = roles.hasPermission(roleNames, RoleMatrixService.MANAGE_COURIER);

        LocalDate today = LocalDate.now(ZONE);
        LocalDate rangeStart = today.minusDays(days - 1L);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthSameDay = lastMonthStart.plusDays(Math.min(today.getDayOfMonth(), lastMonthStart.lengthOfMonth()) - 1L);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", days);
        out.put("generatedAt", LocalDateTime.now(ZONE).toString());
        List<String> sections = new ArrayList<>();

        // ------------------------------------------------------------ orders & sales
        if (canOrders || canMoney) {
            LocalDate from = rangeStart.isBefore(lastMonthStart) ? rangeStart : lastMonthStart;
            double[] dayOrders = new double[days];
            double[] daySales = new double[days];
            double todaySales = 0, yestSales = 0, monthSales = 0, lastMonthSales = 0, rangeSales = 0;
            int todayOrders = 0, yestOrders = 0, monthOrders = 0, lastMonthOrders = 0, rangeOrders = 0, rangeCancelled = 0, rangeReturned = 0, rangeDelivered = 0;
            Map<String, Integer> pay = new TreeMap<>();

            for (Object[] r : repo.orderRows(from.atStartOfDay())) {
                LocalDateTime at = (LocalDateTime) r[0];
                double total = r[1] == null ? 0 : ((Number) r[1]).doubleValue();
                OrderStatus st = (OrderStatus) r[2];
                LocalDate d = at.toLocalDate();
                boolean inRange = !d.isBefore(rangeStart) && !d.isAfter(today);

                if (inRange) {
                    if (st == OrderStatus.CANCELLED) rangeCancelled++;
                    if (st == OrderStatus.RETURNED) rangeReturned++;
                    if (st == OrderStatus.DELIVERED) rangeDelivered++;
                }
                if (!counts(st)) continue;

                if (inRange) {
                    int idx = (int) (d.toEpochDay() - rangeStart.toEpochDay());
                    dayOrders[idx] += 1;
                    daySales[idx] += total;
                    rangeOrders++;
                    rangeSales += total;
                    String pm = r[3] == null ? "OTHER" : r[3].toString();
                    pay.merge(pm, 1, Integer::sum);
                }
                if (d.equals(today)) { todayOrders++; todaySales += total; }
                if (d.equals(today.minusDays(1))) { yestOrders++; yestSales += total; }
                if (!d.isBefore(monthStart) && !d.isAfter(today)) { monthOrders++; monthSales += total; }
                if (!d.isBefore(lastMonthStart) && !d.isAfter(lastMonthSameDay)) { lastMonthOrders++; lastMonthSales += total; }
            }

            List<Map<String, Object>> daily = new ArrayList<>();
            for (int i = 0; i < days; i++) {
                daily.add(m("date", rangeStart.plusDays(i).toString(), "orders", (int) dayOrders[i], "sales", r2(daySales[i])));
            }

            Map<String, Object> sales = new LinkedHashMap<>();
            sales.put("today", m("orders", todayOrders, "sales", r2(todaySales)));
            sales.put("yesterday", m("orders", yestOrders, "sales", r2(yestSales)));
            sales.put("month", m("orders", monthOrders, "sales", r2(monthSales)));
            sales.put("lastMonthSamePeriod", m("orders", lastMonthOrders, "sales", r2(lastMonthSales)));
            sales.put("range", m("orders", rangeOrders, "sales", r2(rangeSales), "avgOrder", rangeOrders == 0 ? 0 : r2(rangeSales / rangeOrders),
                    "cancelled", rangeCancelled, "returned", rangeReturned, "delivered", rangeDelivered));
            sales.put("daily", daily);
            List<Map<String, Object>> payList = new ArrayList<>();
            pay.forEach((k, v) -> payList.add(m("method", k, "orders", v)));
            sales.put("payments", payList);
            out.put("sales", sales);
            sections.add("sales");
        }

        if (canOrders) {
            // open work right now
            Map<String, Long> status = new LinkedHashMap<>();
            for (OrderStatus s : OrderStatus.values()) status.put(s.name(), 0L);
            for (Object[] r : repo.statusCounts()) if (r[0] != null) status.put(((OrderStatus) r[0]).name(), ((Number) r[1]).longValue());
            out.put("pipeline", status);
            out.put("stalePending", repo.stalePending(LocalDateTime.now(ZONE).minusHours(24)));

            List<Map<String, Object>> recent = new ArrayList<>();
            for (Order o : repo.recent(PageRequest.of(0, 8))) {
                recent.add(m("id", o.getId(), "name", o.getName(), "total", o.getTotalAmount() == null ? 0 : r2(o.getTotalAmount()),
                        "status", o.getOrderStatus() == null ? null : o.getOrderStatus().name(),
                        "payment", o.getPaymentMethod() == null ? null : o.getPaymentMethod().name(),
                        "paid", o.getPaymentStatus() == null ? null : o.getPaymentStatus().name(),
                        "city", o.getCity(), "createdAt", o.getCreatedAt() == null ? null : o.getCreatedAt().toString()));
            }
            out.put("recentOrders", recent);

            List<Object[]> top = repo.topProducts(rangeStart.atStartOfDay(), PageRequest.of(0, 6));
            Map<Long, String> imgs = new HashMap<>();
            if (!top.isEmpty()) {
                List<Long> ids = new ArrayList<>();
                for (Object[] r : top) ids.add(((Number) r[0]).longValue());
                for (Object[] r : repo.firstImages(ids)) imgs.put(((Number) r[0]).longValue(), (String) r[1]);
            }
            List<Map<String, Object>> topList = new ArrayList<>();
            for (Object[] r : top) {
                long id = ((Number) r[0]).longValue();
                topList.add(m("id", id, "name", r[1], "qty", ((Number) r[2]).longValue(), "revenue", r2(((Number) r[3]).doubleValue()), "imageUrl", imgs.get(id)));
            }
            out.put("topProducts", topList);
            sections.add("orders");
        }

        // ------------------------------------------------------------ inventory
        if (canProducts) {
            List<Map<String, Object>> low = new ArrayList<>();
            for (Object[] r : repo.lowStockList(PageRequest.of(0, 8))) {
                low.add(m("id", ((Number) r[0]).longValue(), "name", r[1], "sku", r[2], "quantity", ((Number) r[3]).intValue()));
            }
            out.put("inventory", m("published", repo.publishedProducts(), "drafts", repo.draftProducts(),
                    "outOfStock", repo.outOfStockProducts(), "lowStock", repo.lowStockProducts(), "lowStockList", low));
            sections.add("inventory");
        }

        // ------------------------------------------------------------ customers
        if (canUsers) {
            out.put("customers", m("total", repo.customers(), "newInRange", repo.newCustomers(rangeStart.atStartOfDay()),
                    "newToday", repo.newCustomers(today.atStartOfDay())));
            sections.add("customers");
        }

        // ------------------------------------------------------------ money
        if (canMoney) {
            double exp = repo.expensesBetween(monthStart, today);
            double expLast = repo.expensesBetween(lastMonthStart, lastMonthSameDay);
            @SuppressWarnings("unchecked")
            Map<String, Object> sales = (Map<String, Object>) out.get("sales");
            @SuppressWarnings("unchecked")
            double monthSales = ((Number) ((Map<String, Object>) sales.get("month")).get("sales")).doubleValue();
            out.put("finance", m("monthSales", r2(monthSales), "monthExpenses", r2(exp), "monthNet", r2(monthSales - exp), "lastMonthExpenses", r2(expLast)));
            sections.add("finance");
        }

        // ------------------------------------------------------------ couriers
        if (canCourier) {
            Map<String, Long> d = new LinkedHashMap<>();
            for (Object[] r : repo.deliveryCounts()) if (r[0] != null) d.put(r[0].toString(), ((Number) r[1]).longValue());
            out.put("courier", d);
            sections.add("courier");
        }

        out.put("sections", sections);
        return out;
    }
}
