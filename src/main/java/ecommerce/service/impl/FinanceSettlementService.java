package ecommerce.service.impl;

import ecommerce.dto.admin.SettlementsDto;
import ecommerce.entity.Order;
import ecommerce.repository.OrderRepository;
import ecommerce.service.GatewaySettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Online-payment settlements, from real orders: what was paid in the period, the fee your saved gateway rate implies,
 * and what you should receive. There is no bank feed, so "expected" is a calculation to compare with each gateway's statement.
 */
@Service
@RequiredArgsConstructor
public class FinanceSettlementService {

    private final OrderRepository orderRepository;
    private final GatewaySettingService gatewaySettingService;

    @Transactional(readOnly = true)
    public SettlementsDto get(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            LocalDate now = LocalDate.now();
            start = now.withDayOfMonth(1);
            end = now.withDayOfMonth(now.lengthOfMonth());
        }
        var from = start.atStartOfDay();
        var to = end.plusDays(1).atStartOfDay();

        Map<String, SettlementsDto.Gateway> byMethod = new TreeMap<>();
        for (Object[] r : orderRepository.onlinePaidByMethod(from, to)) {
            String m = String.valueOf(r[0]);
            double gross = ((Number) r[2]).doubleValue();
            double rate = gatewaySettingService.getRate(m);
            double fee = round(gross * rate / 100.0);
            SettlementsDto.Gateway g = byMethod.computeIfAbsent(m, k -> new SettlementsDto.Gateway(k, rate, 0, 0, 0, 0, 0, 0));
            g.setPaidCount(((Number) r[1]).longValue());
            g.setPaidGross(round(gross));
            g.setExpectedFee(fee);
            g.setExpectedNet(round(gross - fee));
        }
        double awaitingAmount = 0;
        long awaitingCount = 0;
        for (Object[] r : orderRepository.onlinePendingByMethod()) {
            String m = String.valueOf(r[0]);
            SettlementsDto.Gateway g = byMethod.computeIfAbsent(m,
                    k -> new SettlementsDto.Gateway(k, gatewaySettingService.getRate(k), 0, 0, 0, 0, 0, 0));
            g.setAwaitingCount(((Number) r[1]).longValue());
            g.setAwaitingAmount(round(((Number) r[2]).doubleValue()));
            awaitingCount += g.getAwaitingCount();
            awaitingAmount += g.getAwaitingAmount();
        }

        double gross = 0, fees = 0, net = 0;
        for (SettlementsDto.Gateway g : byMethod.values()) {
            gross += g.getPaidGross();
            fees += g.getExpectedFee();
            net += g.getExpectedNet();
        }

        List<SettlementsDto.Payment> payments = new ArrayList<>();
        for (Order o : orderRepository.findOnlinePaid(from, to, PageRequest.of(0, 100))) {
            double g = o.getTotalAmount() != null ? o.getTotalAmount() : 0;
            String m = String.valueOf(o.getPaymentMethod());
            double fee = round(g * gatewaySettingService.getRate(m) / 100.0);
            payments.add(new SettlementsDto.Payment(
                    o.getId(),
                    o.getInvoice() != null ? o.getInvoice().getInvoiceNumber() : null,
                    o.getName(), m, g, fee, round(g - fee),
                    o.getPaidAt() != null ? o.getPaidAt().toString() : null));
        }

        long totalPaid = byMethod.values().stream().mapToLong(SettlementsDto.Gateway::getPaidCount).sum();
        return new SettlementsDto(start.toString(), end.toString(), round(gross), round(fees), round(net),
                round(awaitingAmount), awaitingCount, new ArrayList<>(byMethod.values()), payments, totalPaid);
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
