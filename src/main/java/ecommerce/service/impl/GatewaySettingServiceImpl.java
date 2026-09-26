package ecommerce.service.impl;

import ecommerce.dto.admin.GatewaySettingDto;
import ecommerce.entity.GatewaySetting;
import ecommerce.enums.PaymentMethod;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.GatewaySettingRepository;
import ecommerce.repository.OrderRepository;
import ecommerce.service.GatewaySettingService;
import ecommerce.utils.GatewayFeeRates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GatewaySettingServiceImpl implements GatewaySettingService {

    private final GatewaySettingRepository gatewaySettingRepository;
    private final OrderRepository orderRepository;

    private static final Map<String, String> DEFAULT_CYCLE = Map.of(
            "BKASH", "T+2", "NAGAD", "T+2", "ROCKET", "T+3", "CARD", "T+3", "COD", "On remit"
    );

    @Override
    @Transactional
    public double getRate(String paymentMethod) {
        try {
            PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
            return gatewaySettingRepository.findByPaymentMethod(method)
                    .map(GatewaySetting::getFeeRatePct)
                    .orElseGet(() -> GatewayFeeRates.rateFor(paymentMethod));
        } catch (IllegalArgumentException e) {
            return GatewayFeeRates.rateFor(paymentMethod);
        }
    }

    @Override
    @Transactional
    public List<GatewaySettingDto> getAllWithStats() {
        ensureSeeded();

        LocalDate now = LocalDate.now();
        LocalDate start = now.withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());
        LocalDateTime startDT = start.atStartOfDay();
        LocalDateTime endDT = end.plusDays(1).atStartOfDay();

        Map<String, double[]> stats = new HashMap<>(); // method -> [amount, count]
        for (Object[] row : orderRepository.gatewayMix(startDT, endDT)) {
            String method = row[0] != null ? row[0].toString() : "COD";
            stats.put(method, new double[]{((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue()});
        }

        List<GatewaySettingDto> result = new ArrayList<>();
        for (GatewaySetting gs : gatewaySettingRepository.findAll()) {
            GatewaySettingDto dto = new GatewaySettingDto();
            dto.setPaymentMethod(gs.getPaymentMethod().name());
            dto.setFeeRatePct(gs.getFeeRatePct());
            dto.setSettlementCycle(gs.getSettlementCycle());
            dto.setConnected(gs.isConnected());
            double[] s = stats.getOrDefault(gs.getPaymentMethod().name(), new double[]{0, 0});
            dto.setCollectedThisMonth(s[0]);
            dto.setOrderCount((long) s[1]);
            result.add(dto);
        }
        result.sort((a, b) -> Double.compare(b.getCollectedThisMonth(), a.getCollectedThisMonth()));
        return result;
    }

    @Override
    @Transactional
    public GatewaySettingDto updateRate(String paymentMethod, double feeRatePct) {
        if (feeRatePct < 0 || feeRatePct > 100) {
            throw new BadRequestException("Fee rate must be between 0 and 100");
        }
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(paymentMethod);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid payment method: " + paymentMethod);
        }

        GatewaySetting gs = gatewaySettingRepository.findByPaymentMethod(method).orElseGet(() -> {
            GatewaySetting created = new GatewaySetting();
            created.setPaymentMethod(method);
            created.setSettlementCycle(DEFAULT_CYCLE.getOrDefault(method.name(), "—"));
            return created;
        });
        gs.setFeeRatePct(feeRatePct);
        gatewaySettingRepository.save(gs);

        GatewaySettingDto dto = new GatewaySettingDto();
        dto.setPaymentMethod(method.name());
        dto.setFeeRatePct(feeRatePct);
        dto.setSettlementCycle(gs.getSettlementCycle());
        dto.setConnected(gs.isConnected());
        return dto;
    }

    private void ensureSeeded() {
        for (PaymentMethod method : PaymentMethod.values()) {
            if (gatewaySettingRepository.findByPaymentMethod(method).isEmpty()) {
                GatewaySetting gs = new GatewaySetting();
                gs.setPaymentMethod(method);
                gs.setFeeRatePct(GatewayFeeRates.rateFor(method.name()));
                gs.setSettlementCycle(DEFAULT_CYCLE.getOrDefault(method.name(), "—"));
                gs.setConnected(true);
                gatewaySettingRepository.save(gs);
            }
        }
    }
}
