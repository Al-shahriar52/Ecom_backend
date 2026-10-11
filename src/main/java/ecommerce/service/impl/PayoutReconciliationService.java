package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.dto.courier.PayoutReconciliationDto;
import ecommerce.dto.courier.PayoutReconciliationDto.Line;
import ecommerce.entity.Delivery;
import ecommerce.entity.Invoice;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.DeliveryRepository;
import ecommerce.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Matches the parcels in one Steadfast payout against our invoices. Read-only: it shows the gaps and changes nothing.
 * Steadfast's per-parcel field names are not spelled out in the docs, so the reader looks for the usual ones.
 */
@Service
@RequiredArgsConstructor
public class PayoutReconciliationService {

    private final SteadfastService steadfast;
    private final InvoiceRepository invoices;
    private final DeliveryRepository deliveries;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PayoutReconciliationDto reconcile(String paymentId) {
        JsonNode body;
        try {
            body = steadfast.getPaymentDetail(paymentId);
        } catch (Exception e) {
            throw new BadRequestException("Could not read this payout from Steadfast: " + e.getMessage());
        }
        JsonNode parcels = firstObjectArray(body, 0);
        if (parcels == null) throw new BadRequestException("Steadfast returned no parcel list for this payout.");

        List<Line> lines = new ArrayList<>();
        List<String> invoiceNumbers = new ArrayList<>();
        for (JsonNode p : parcels) {
            String inv = text(p, "invoice", "invoice_number", "merchant_invoice");
            if (inv != null) invoiceNumbers.add(inv);
        }
        Map<String, Invoice> byNumber = new HashMap<>();
        if (!invoiceNumbers.isEmpty()) {
            for (Invoice i : invoices.findByInvoiceNumberIn(invoiceNumbers)) byNumber.put(i.getInvoiceNumber(), i);
        }

        double settled = 0, expected = 0;
        int matched = 0;
        for (JsonNode p : parcels) {
            String inv = text(p, "invoice", "invoice_number", "merchant_invoice");
            String cid = text(p, "consignment_id", "cid", "id");
            Double amt = number(p, "collected_amount", "cod_amount", "collected", "amount", "cod");
            Line l = new Line(inv, cid, text(p, "delivery_status", "status"), amt, null, null, null, null);
            Invoice i = inv != null ? byNumber.get(inv) : null;
            if (i != null && i.getOrder() != null) {
                matched++;
                l.setOrderId(i.getOrder().getId());
                l.setInvoiceTotal(i.getTotalAmount());
                if (amt != null && i.getTotalAmount() != null) l.setDifference(round(amt - i.getTotalAmount()));
                if (i.getTotalAmount() != null) expected += i.getTotalAmount();
                Delivery d = i.getOrder().getDelivery();
                if (d != null && d.getDeliveryStatus() != null) l.setOurDeliveryStatus(d.getDeliveryStatus().name());
            }
            if (amt != null) settled += amt;
            lines.add(l);
        }
        return new PayoutReconciliationDto(paymentId, lines.size(), matched, round(settled), round(expected), lines);
    }

    /** First array of objects found in the response (depth-limited), wherever Steadfast nests it. */
    private static JsonNode firstObjectArray(JsonNode n, int depth) {
        if (n == null || depth > 4) return null;
        if (n.isArray()) return (n.size() > 0 && n.get(0).isObject()) ? n : null;
        if (n.isObject()) {
            Iterator<JsonNode> it = n.elements();
            while (it.hasNext()) {
                JsonNode r = firstObjectArray(it.next(), depth + 1);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static String text(JsonNode n, String... keys) {
        for (String k : keys) if (n.hasNonNull(k) && n.get(k).isValueNode()) return n.get(k).asText();
        return null;
    }

    private static Double number(JsonNode n, String... keys) {
        for (String k : keys) {
            if (n.hasNonNull(k) && n.get(k).isValueNode()) {
                try { return Double.parseDouble(n.get(k).asText().replace(",", "")); } catch (NumberFormatException ignored) { }
            }
        }
        return null;
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
