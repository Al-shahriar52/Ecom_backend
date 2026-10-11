package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.dto.courier.SteadfastPayoutsDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Reads what Steadfast owes us and the payouts it has made. A failure on one call doesn't hide the other. */
@Service
@RequiredArgsConstructor
public class SteadfastPayoutService {

    private static final Logger log = LoggerFactory.getLogger(SteadfastPayoutService.class);
    private final SteadfastService steadfast;

    public SteadfastPayoutsDto overview(int page) {
        SteadfastPayoutsDto dto = new SteadfastPayoutsDto();
        dto.setPage(Math.max(1, page));
        dto.setPayouts(new ArrayList<>());

        try {
            JsonNode b = steadfast.getBalance();
            if (b != null && b.hasNonNull("current_balance")) dto.setBalance(b.get("current_balance").asDouble());
            else dto.setBalanceError("Steadfast returned no balance.");
        } catch (Exception e) {
            log.warn("Steadfast balance failed: {}", e.getMessage());
            dto.setBalanceError("Could not read balance: " + e.getMessage());
        }

        try {
            JsonNode p = steadfast.getPayments(page);
            JsonNode list = p != null ? p.get("payments") : null;
            List<SteadfastPayoutsDto.Payout> out = new ArrayList<>();
            if (list != null && list.isArray()) {
                for (JsonNode n : list) {
                    out.add(new SteadfastPayoutsDto.Payout(
                            text(n, "payment_id"), n.path("amount").asDouble(0), text(n, "method"),
                            n.path("charges").asDouble(0), text(n, "status_label"),
                            text(n, "created_at"), text(n, "paid_at")));
                }
            }
            dto.setPayouts(out);
        } catch (Exception e) {
            log.warn("Steadfast payments failed: {}", e.getMessage());
            dto.setPayoutsError("Could not read payouts: " + e.getMessage());
        }
        return dto;
    }

    private static String text(JsonNode n, String f) {
        return n.hasNonNull(f) ? n.get(f).asText() : null;
    }
}
