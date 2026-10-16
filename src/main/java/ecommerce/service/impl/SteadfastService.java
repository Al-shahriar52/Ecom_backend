package ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.courier.PickupOutcome;
import ecommerce.entity.Invoice;
import ecommerce.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class SteadfastService {
    @Value("${steadfast.api.key}")
    private String apiKey;

    @Value("${steadfast.api.secret}")
    private String secretKey;

    @Value("${steadfast.api.bulk-pickup-endpoint}")
    private String bulkPickupEndpoint;

    @Value("${steadfast.api.single-pickup-endpoint}")
    private String singlePickupEndpoint;

    @Value("${steadfast.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Police stations almost never change - cache for 12h to spare the API.
    private volatile JsonNode policeStationCache;
    private volatile long policeStationCachedAt = 0;
    private static final long POLICE_CACHE_MS = 12L * 60 * 60 * 1000;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        headers.set("Secret-Key", secretKey);
        return headers;
    }

    /** POST /create_pickup_request. 201 = created; 409 PICKUP_REQUEST_EXISTS = one already pending (safe to retry). */
    public PickupOutcome createPickupRequest(Long addressId, Long policeStationId, String address,
                                             String contactNumber, String note, Integer estimQty) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("address_id", addressId);
        payload.put("police_station_id", policeStationId);
        payload.put("address", address);
        payload.put("contact_number", contactNumber);
        if (note != null && !note.isBlank()) payload.put("note", note);
        if (estimQty != null) payload.put("estim_qty", estimQty);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    baseUrl + "/create_pickup_request", new HttpEntity<>(payload, authHeaders()), JsonNode.class);
            JsonNode body = response.getBody();
            String id = null;
            if (body != null) {
                JsonNode data = body.has("data") ? body.get("data") : body;
                if (data != null && data.has("id")) id = data.get("id").asText();
            }
            String msg = body != null && body.has("message") ? body.get("message").asText() : "Pickup request submitted";
            return new PickupOutcome("SUBMITTED", id, msg);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String text = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 409 || (text != null && text.contains("PICKUP_REQUEST_EXISTS"))) {
                return new PickupOutcome("ALREADY_PENDING", null,
                        "A pickup request is already pending - the rider will collect all your parcels in one visit.");
            }
            String msg = switch (e.getStatusCode().value()) {
                case 401, 403 -> "Steadfast rejected the API credentials";
                case 422 -> "Steadfast rejected the details: " + text;
                case 429 -> "Steadfast rate limit hit - try again in a minute";
                default -> "Steadfast error " + e.getStatusCode().value() + ": " + text;
            };
            return new PickupOutcome("FAILED", null, msg);
        } catch (Exception e) {
            return new PickupOutcome("FAILED", null, "Could not reach Steadfast: " + e.getMessage());
        }
    }

    /** GET /status_by_cid/{id} -> {status:200, delivery_status:"..."} (answers are cached 60s by Steadfast). */
    public JsonNode getStatusByCid(String consignmentId) {
        ResponseEntity<JsonNode> res = restTemplate.exchange(baseUrl + "/status_by_cid/" + consignmentId,
                HttpMethod.GET, new HttpEntity<>(authHeaders()), JsonNode.class);
        return res.getBody();
    }

    /** GET /trackings_by_invoice/{invoice} -> {status:200, tracking:[{tracking_type, text, created_at}]} */
    public JsonNode getTrackingByInvoice(String invoice) {
        ResponseEntity<JsonNode> res = restTemplate.exchange(baseUrl + "/trackings_by_invoice/" + invoice,
                HttpMethod.GET, new HttpEntity<>(authHeaders()), JsonNode.class);
        return res.getBody();
    }

    /** GET /fraud_check/score/{phone}: the customer's delivery record across merchants. 429 when over the rate limit. */
    public JsonNode getFraudCheck(String phone) {
        return restTemplate.exchange(baseUrl + "/fraud_check/score/" + phone, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), JsonNode.class).getBody();
    }

    /** GET /payments/{id}: one payout with every parcel it settled (digits only; "SFC-88213" == "88213"). */
    public JsonNode getPaymentDetail(String paymentId) {
        String digits = paymentId == null ? "" : paymentId.replaceAll("\\D", "");
        return restTemplate.exchange(baseUrl + "/payments/" + digits, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), JsonNode.class).getBody();
    }

    /** GET /get_balance -> {status:200, current_balance}. What Steadfast currently owes us. */
    public JsonNode getBalance() {
        return restTemplate.exchange(baseUrl + "/get_balance", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), JsonNode.class).getBody();
    }

    /** GET /payments?page=N -> {payments:[{payment_id, amount, method, charges, status_label, paid_at...}]}, ten per page. */
    public JsonNode getPayments(int page) {
        return restTemplate.exchange(baseUrl + "/payments?page=" + Math.max(1, page), HttpMethod.GET,
                new HttpEntity<>(authHeaders()), JsonNode.class).getBody();
    }

    /** GET /police_stations (cached). */
    public JsonNode getPoliceStations() {
        long now = System.currentTimeMillis();
        if (policeStationCache != null && now - policeStationCachedAt < POLICE_CACHE_MS) {
            return policeStationCache;
        }
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/police_stations", HttpMethod.GET, new HttpEntity<>(authHeaders()), JsonNode.class);
            JsonNode body = response.getBody();
            JsonNode list = body != null && body.has("data") ? body.get("data") : body;
            policeStationCache = list;
            policeStationCachedAt = now;
            return list;
        } catch (Exception e) {
            if (policeStationCache != null) return policeStationCache; // stale beats nothing
            throw new RuntimeException("Failed to load police stations from Steadfast: " + e.getMessage());
        }
    }

    public JsonNode sendBulkPickupRequest(List<Order> orders, Map<Long, Invoice> invoiceMap) {
        return sendBulkPickupRequest(orders, invoiceMap, null);
    }

    /** codOverrides: orderId -> amount to collect (only used for COD orders). */
    public JsonNode sendBulkPickupRequest(List<Order> orders, Map<Long, Invoice> invoiceMap, Map<Long, Double> codOverrides) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        headers.set("Secret-Key", secretKey);

        // 1. Build the list of order objects
        List<Map<String, Object>> orderDataList = new ArrayList<>();
        
        for (Order order : orders) {
            Invoice invoice = invoiceMap.get(order.getId());
            if (invoice == null) continue;

            Map<String, Object> item = new HashMap<>();
            // Steadfast uses 'invoice' as their unique identifier mapping
            item.put("invoice", invoice.getInvoiceNumber()); 
            item.put("recipient_name", order.getName() != null ? order.getName() : "N/A");
            item.put("recipient_address", order.getShippingAddress() + ", " + order.getArea() + ", " + order.getCity());
            item.put("recipient_phone", order.getPhoneNumber() != null ? order.getPhoneNumber() : "");
            
            double codAmount = order.getPaymentMethod() == ecommerce.enums.PaymentMethod.COD
                    ? (codOverrides != null && codOverrides.get(order.getId()) != null ? codOverrides.get(order.getId()) : invoice.getTotalAmount())
                    : 0.0;
            item.put("cod_amount", codAmount);
            item.put("note", order.getOrderNote());
            
            orderDataList.add(item);
        }

        // 2. Wrap in 'data' object as per their PHP doc example
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", orderDataList);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            // Using JsonNode handles both direct Arrays [ ] and Objects { "data": [ ] }
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(bulkPickupEndpoint, request, JsonNode.class);
            return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Steadfast API: " + e.getMessage());
        }
    }

    public JsonNode sendSinglePickupRequest(Order order, Invoice invoice) {
        return sendSinglePickupRequest(order, invoice, null);
    }

    /** codOverride: amount to collect instead of the invoice total (only used for COD orders). */
    public JsonNode sendSinglePickupRequest(Order order, Invoice invoice, Double codOverride) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        headers.set("Secret-Key", secretKey);

        if (invoice == null) {
            throw new IllegalArgumentException("Invoice cannot be null");
        }

        // Build the single order payload directly (No need to wrap in a 'data' array)
        Map<String, Object> payload = new HashMap<>();
        payload.put("invoice", invoice.getInvoiceNumber());
        payload.put("recipient_name", order.getName() != null ? order.getName() : "N/A");
        payload.put("recipient_address", order.getShippingAddress() + ", " + order.getArea() + ", " + order.getCity());
        payload.put("recipient_phone", order.getPhoneNumber() != null ? order.getPhoneNumber() : "");

        double codAmount = order.getPaymentMethod() == ecommerce.enums.PaymentMethod.COD
                ? (codOverride != null ? codOverride : invoice.getTotalAmount())
                : 0.0;
        payload.put("cod_amount", codAmount);
        payload.put("note", order.getOrderNote());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            // Assuming you have a variable like singlePickupEndpoint configured similarly to bulkPickupEndpoint
            // Usually, Steadfast's single endpoint ends in /create_order
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(singlePickupEndpoint, request, JsonNode.class);
            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Steadfast Single API: " + e.getMessage());
        }
    }
}