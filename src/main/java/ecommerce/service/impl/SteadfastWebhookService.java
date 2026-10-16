package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.courier.CourierStatus;
import ecommerce.courier.SteadfastProvider;
import ecommerce.entity.WebhookEvent;
import ecommerce.enums.DeliveryStatus;
import ecommerce.repository.DeliveryRepository;
import ecommerce.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/**
 * Receives Steadfast webhooks. Every request is authenticated (Bearer token + HMAC-SHA256 of the raw body),
 * de-duplicated by Idempotency-Key, stored raw, and, when it names a known parcel and a status, applied.
 */
@Service
@RequiredArgsConstructor
public class SteadfastWebhookService {

    private static final Logger log = LoggerFactory.getLogger(SteadfastWebhookService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebhookEventRepository events;
    private final DeliveryRepository deliveries;
    private final DeliveryStatusApplier applier;

    @Value("${steadfast.webhook.token:}")
    private String token;

    public enum Result { OK, DUPLICATE, UNAUTHORIZED, NOT_CONFIGURED }

    public Result handle(String authorization, String signature, String idempotencyKey, byte[] rawBody) {
        if (token == null || token.isBlank()) return Result.NOT_CONFIGURED;

        boolean bearerOk = authorization != null && authorization.startsWith("Bearer ")
                && constantEq(authorization.substring(7).trim(), token);
        boolean sigOk = signature != null && constantEq(signature.trim().toLowerCase(), hmacHex(rawBody, token));
        if (!bearerOk || !sigOk) return Result.UNAUTHORIZED;

        String raw = new String(rawBody, StandardCharsets.UTF_8);
        String key = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey.trim() : "sha:" + sha256(raw);
        if (events.existsByProviderAndIdempotencyKey("Steadfast", key)) return Result.DUPLICATE;

        WebhookEvent ev = new WebhookEvent();
        ev.setProvider("Steadfast");
        ev.setIdempotencyKey(key);
        ev.setPayload(raw);
        ev.setReceivedAt(LocalDateTime.now());
        ev.setOutcome("LOGGED_ONLY");

        try {
            JsonNode root = MAPPER.readTree(raw);
            ev.setEventType(firstText(root, "event", "type", "event_type"));
            String cid = firstText(root, "consignment_id", "consignmentId", "cid", "consignment.consignment_id", "data.consignment_id", "parcel.consignment_id");
            String status = firstText(root, "delivery_status", "status", "data.delivery_status", "data.status", "parcel.delivery_status", "consignment.status");
            ev.setConsignmentId(cid);

            if (cid != null && status != null) {
                var delivery = deliveries.findFirstByCourierNameAndConsignmentId("Steadfast", cid);
                if (delivery.isEmpty()) {
                    ev.setOutcome("UNMATCHED");
                } else {
                    DeliveryStatus mapped = SteadfastProvider.mapStatus(status);
                    String riderName = firstText(root, "rider_name", "rider.name", "delivery_man_name", "delivery_man.name", "deliveryman_name", "driver_name",
                            "data.rider_name", "data.rider.name", "data.delivery_man_name", "parcel.rider_name");
                    String riderPhone = firstText(root, "rider_phone", "rider.phone", "rider_mobile", "rider.mobile", "delivery_man_phone", "delivery_man.phone",
                            "deliveryman_phone", "driver_phone", "data.rider_phone", "data.rider.phone", "data.delivery_man_phone", "parcel.rider_phone");
                    boolean changed = applier.apply(delivery.get().getId(), new CourierStatus(mapped, status, null), "WEBHOOK", riderName, riderPhone);
                    ev.setOutcome(changed ? "APPLIED" : "NO_CHANGE");
                }
            }
        } catch (Exception e) {
            // Still acknowledged: the raw payload is saved above, so nothing is lost and retries would not help.
            log.warn("Steadfast webhook could not be applied: {}", e.getMessage());
        }
        events.save(ev);
        return Result.OK;
    }

    private static String firstText(JsonNode root, String... paths) {
        for (String p : paths) {
            JsonNode n = root;
            for (String part : p.split("\\.")) n = n == null ? null : n.get(part);
            if (n != null && !n.isNull() && n.isValueNode() && !n.asText().isBlank()) return n.asText();
        }
        return null;
    }

    static String hmacHex(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(body));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(String s) {
        try {
            return toHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static boolean constantEq(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
