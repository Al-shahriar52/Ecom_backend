package ecommerce.service.impl;

import ecommerce.dto.admin.AdminCodParcelDto;
import ecommerce.dto.admin.CodSummaryDto;
import ecommerce.dto.admin.CourierCodStatDto;
import ecommerce.dto.admin.CourierMixItemDto;
import ecommerce.entity.Order;
import ecommerce.repository.OrderRepository;
import ecommerce.service.FinanceCodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceCodServiceImpl implements FinanceCodService {

    private final OrderRepository orderRepository;

    @Override
    public CodSummaryDto getCodSummary() {
        double outAmount = orderRepository.sumCodPending();
        long outCount = orderRepository.countCodPending();
        double unpaidAmount = orderRepository.sumCodDeliveredUnpaid();
        long unpaidCount = orderRepository.countCodDeliveredUnpaid();
        double returnedAmount = orderRepository.sumCodReturned();
        long returnedCount = orderRepository.countCodReturned();
        long totalCod = orderRepository.countAllCodWithDelivery();

        double returnRate = totalCod > 0 ? ((double) returnedCount / totalCod) * 100 : 0;

        List<Object[]> mixRows = orderRepository.codCourierMix();
        List<CourierMixItemDto> courierMix = new ArrayList<>();
        for (Object[] row : mixRows) {
            courierMix.add(new CourierMixItemDto((String) row[0], ((Number) row[1]).doubleValue(), ((Number) row[2]).longValue()));
        }
        courierMix.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));

        List<Order> orders = orderRepository.findCodOrders(PageRequest.of(0, 50));
        List<AdminCodParcelDto> parcels = orders.stream().map(this::mapParcel).collect(Collectors.toList());

        return new CodSummaryDto(
                outAmount, outCount, unpaidAmount, unpaidCount, returnedAmount, returnedCount,
                returnRate, courierMix, parcels, courierStats()
        );
    }

    private List<CourierCodStatDto> courierStats() {
        Map<String, double[]> acc = new LinkedHashMap<>(); // [total, holdAmt, holdCnt, unpaidAmt, unpaidCnt, retAmt, retCnt]
        for (Object[] row : orderRepository.codCourierBreakdown()) {
            String courier = (String) row[0];
            String status = row[1] != null ? row[1].toString() : "";
            String pay = row[2] != null ? row[2].toString() : "";
            long cnt = ((Number) row[3]).longValue();
            double sum = ((Number) row[4]).doubleValue();
            double[] a = acc.computeIfAbsent(courier, k -> new double[7]);
            a[0] += cnt;
            if (status.equals("READY_FOR_PICKUP") || status.equals("IN_TRANSIT")) { a[1] += sum; a[2] += cnt; }
            else if (status.equals("DELIVERED") && !pay.equals("PAID")) { a[3] += sum; a[4] += cnt; }
            else if (status.equals("RETURNED")) { a[5] += sum; a[6] += cnt; }
        }
        List<CourierCodStatDto> out = new ArrayList<>();
        for (Map.Entry<String, double[]> e : acc.entrySet()) {
            double[] a = e.getValue();
            out.add(new CourierCodStatDto(e.getKey(), (long) a[0], a[1], (long) a[2], a[3], (long) a[4], a[5], (long) a[6],
                    a[0] > 0 ? a[6] / a[0] * 100 : 0));
        }
        out.sort((x, y) -> Double.compare(y.getHoldingAmount() + y.getUnpaidAmount(), x.getHoldingAmount() + x.getUnpaidAmount()));
        return out;
    }

    private AdminCodParcelDto mapParcel(Order order) {
        AdminCodParcelDto dto = new AdminCodParcelDto();
        dto.setOrderId(order.getId());
        dto.setInvoice(order.getInvoice() != null ? order.getInvoice().getInvoiceNumber() : null);
        dto.setCustomer(order.getName());
        double collectable = order.getDelivery() != null && order.getDelivery().getCodAmount() != null
                ? order.getDelivery().getCodAmount()
                : (order.getTotalAmount() != null ? order.getTotalAmount() : 0);
        dto.setCollectable(collectable);
        if (order.getDelivery() != null) {
            dto.setConsignmentId(order.getDelivery().getConsignmentId());
            dto.setCourierName(order.getDelivery().getCourierName());
            dto.setDeliveryStatus(order.getDelivery().getDeliveryStatus() != null ? order.getDelivery().getDeliveryStatus().name() : null);
        }
        return dto;
    }
}
