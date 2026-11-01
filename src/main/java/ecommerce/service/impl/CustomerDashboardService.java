package ecommerce.service.impl;

import ecommerce.entity.Delivery;
import ecommerce.entity.Order;
import ecommerce.entity.User;
import ecommerce.enums.OrderStatus;
import ecommerce.repository.CustomerDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Summary for the signed-in customer's "My account" home page. */
@Service
@RequiredArgsConstructor
public class CustomerDashboardService {

    private final CustomerDashboardRepository repo;

    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) out.put((String) kv[i], kv[i + 1]);
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(User user) {
        Long uid = user.getId();

        long orders = 0, delivered = 0, cancelled = 0, inProgress = 0;
        double spent = 0;
        for (Object[] r : repo.statusTotals(uid)) {
            OrderStatus st = (OrderStatus) r[0];
            long c = ((Number) r[1]).longValue();
            double sum = ((Number) r[2]).doubleValue();
            orders += c;
            if (st == OrderStatus.DELIVERED) delivered += c;
            if (st == OrderStatus.CANCELLED || st == OrderStatus.RETURNED) cancelled += c;
            if (st == OrderStatus.PENDING || st == OrderStatus.CONFIRMED || st == OrderStatus.PACKAGED || st == OrderStatus.SHIPPED) inProgress += c;
            if (st != OrderStatus.CANCELLED && st != OrderStatus.RETURNED) spent += sum;
        }

        List<Order> recent = repo.latest(uid, PageRequest.of(0, 5));
        List<Order> open = repo.openOrders(uid, PageRequest.of(0, 1));

        // items + first image for every order we show
        Set<Long> orderIds = new LinkedHashSet<>();
        recent.forEach(o -> orderIds.add(o.getId()));
        open.forEach(o -> orderIds.add(o.getId()));
        Map<Long, List<Map<String, Object>>> items = new HashMap<>();
        Set<Long> productIds = new LinkedHashSet<>();
        if (!orderIds.isEmpty()) {
            for (Object[] r : repo.itemsOf(orderIds)) {
                long pid = ((Number) r[1]).longValue();
                productIds.add(pid);
                items.computeIfAbsent(((Number) r[0]).longValue(), k -> new ArrayList<>())
                        .add(m("productId", pid, "name", r[2], "quantity", ((Number) r[3]).intValue(), "slug", r[4]));
            }
        }
        Map<Long, String> imgs = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Object[] r : repo.firstImages(productIds)) imgs.put(((Number) r[0]).longValue(), (String) r[1]);
        }

        List<Map<String, Object>> recentOut = new ArrayList<>();
        for (Order o : recent) recentOut.add(orderCard(o, items.get(o.getId()), imgs));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", user.getName());
        out.put("memberSince", user.getCreatedAt() == null ? null : user.getCreatedAt().toString());
        out.put("stats", m("orders", orders, "spent", r2(spent), "delivered", delivered, "inProgress", inProgress, "cancelled", cancelled,
                "addresses", repo.addressCount(uid), "wishlist", repo.wishlistCount(uid)));
        out.put("recentOrders", recentOut);

        if (!open.isEmpty()) {
            Order o = open.get(0);
            Map<String, Object> active = orderCard(o, items.get(o.getId()), imgs);
            active.put("confirmedAt", str(o.getConfirmedAt()));
            active.put("packagedAt", str(o.getPackagedAt()));
            active.put("shippedAt", str(o.getShippedAt()));
            active.put("paymentMethod", o.getPaymentMethod() == null ? null : o.getPaymentMethod().name());
            active.put("paymentStatus", o.getPaymentStatus() == null ? null : o.getPaymentStatus().name());
            List<Delivery> dl = repo.deliveryOf(o.getId());
            if (!dl.isEmpty()) {
                Delivery d = dl.get(0);
                active.put("courier", m("name", d.getCourierName(), "trackingCode", d.getTrackingCode() != null ? d.getTrackingCode() : d.getConsignmentId(),
                        "riderName", d.getRiderName(), "riderPhone", d.getRiderPhone()));
            }
            out.put("activeOrder", active);
        }
        return out;
    }

    private static String str(Object t) { return t == null ? null : t.toString(); }

    private Map<String, Object> orderCard(Order o, List<Map<String, Object>> its, Map<Long, String> imgs) {
        List<Map<String, Object>> list = its == null ? List.of() : its;
        List<Map<String, Object>> thumbs = new ArrayList<>();
        int count = 0;
        for (Map<String, Object> it : list) {
            count += (Integer) it.get("quantity");
            if (thumbs.size() < 4) thumbs.add(m("name", it.get("name"), "imageUrl", imgs.get((Long) it.get("productId"))));
        }
        return m("id", o.getId(), "status", o.getOrderStatus() == null ? null : o.getOrderStatus().name(),
                "total", o.getTotalAmount() == null ? 0 : r2(o.getTotalAmount()), "createdAt", str(o.getCreatedAt()),
                "itemCount", count, "firstItem", list.isEmpty() ? null : list.get(0).get("name"), "thumbs", thumbs);
    }
}
