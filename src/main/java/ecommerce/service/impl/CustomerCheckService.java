package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.dto.courier.CustomerCheckDto;
import ecommerce.exceptionHandling.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up a customer's cross-merchant delivery record on Steadfast. Run on demand from the order page
 * (the endpoint is rate limited), and cached for six hours per number.
 */
@Service
@RequiredArgsConstructor
public class CustomerCheckService {

    private static final long TTL_MS = 6L * 60 * 60 * 1000;
    private final SteadfastService steadfast;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(CustomerCheckDto dto, long at) {}

    public CustomerCheckDto check(String rawPhone) {
        String phone = normalize(rawPhone);
        if (phone == null) throw new BadRequestException("That does not look like a Bangladeshi mobile number.");

        Cached c = cache.get(phone);
        if (c != null && System.currentTimeMillis() - c.at() < TTL_MS) return c.dto();

        JsonNode body;
        try {
            body = steadfast.getFraudCheck(phone);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 429) {
                throw new BadRequestException("Steadfast's customer-check limit is reached for now. Try again in a few minutes.");
            }
            throw new BadRequestException("Steadfast could not check this number (" + e.getStatusCode().value() + ").");
        } catch (Exception e) {
            throw new BadRequestException("Could not reach Steadfast: " + e.getMessage());
        }
        if (body == null) throw new BadRequestException("Steadfast returned nothing for this number.");

        CustomerCheckDto dto = build(phone, body);
        cache.put(phone, new Cached(dto, System.currentTimeMillis()));
        return dto;
    }

    static CustomerCheckDto build(String phone, JsonNode b) {
        Integer del = b.hasNonNull("delivery_ratio") ? b.get("delivery_ratio").asInt() : null;
        Integer can = b.hasNonNull("cancellation_ratio") ? b.get("cancellation_ratio").asInt() : null;
        String band = b.hasNonNull("volume_band") ? b.get("volume_band").asText() : null;
        int reports = b.path("total_reports").asInt(0);

        Map<String, Integer> cats = new LinkedHashMap<>();
        JsonNode fc = b.get("fraud_categories");
        if (fc != null && fc.isObject()) fc.fields().forEachRemaining(e -> cats.put(e.getKey(), e.getValue().asInt()));

        String verdict;
        String summary;
        if (del == null && reports == 0) {
            verdict = "UNKNOWN";
            summary = "No finished parcels on record for this number yet. Not a bad sign, but nothing to go on.";
        } else if (reports > 0 || (del != null && del < 60)) {
            verdict = "RISKY";
            summary = (reports > 0 ? reports + " report" + (reports == 1 ? "" : "s") + " from other merchants. " : "")
                    + (del != null ? del + "% of finished parcels were delivered." : "");
        } else if ((del != null && del < 80) || (can != null && can >= 25)) {
            verdict = "CAUTION";
            summary = (del != null ? del + "% delivered" : "Delivery rate unknown")
                    + (can != null ? ", " + can + "% cancelled." : ".");
        } else {
            verdict = "GOOD";
            summary = del + "% of finished parcels delivered" + (can != null ? ", " + can + "% cancelled." : ".");
        }
        return new CustomerCheckDto(phone, verdict, summary.trim(), del, can, band, reports, cats);
    }

    /** 01XXXXXXXXX, from +8801..., 8801..., or 1XXXXXXXXX. */
    static String normalize(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("\\D", "");
        if (d.startsWith("880")) d = d.substring(2);
        if (d.length() == 10 && d.startsWith("1")) d = "0" + d;
        return d.matches("01[3-9]\\d{8}") ? d : null;
    }
}
