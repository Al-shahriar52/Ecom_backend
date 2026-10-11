package ecommerce.courier.pathao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.entity.CourierAccount;
import ecommerce.repository.CourierAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Low-level Pathao Merchant API client. Handles OAuth2: the access token (valid ~5 days) is stored in
 * courier_accounts, refreshed with the refresh token, and re-issued with username/password if refresh fails.
 */
@Component
public class PathaoClient {

    private static final String PROVIDER = "pathao";
    private static final String API = "/aladdin/api/v1";

    @Value("${pathao.base-url:https://courier-api-sandbox.pathao.com}")
    private String baseUrl;
    @Value("${pathao.client-id:}")
    private String clientId;
    @Value("${pathao.client-secret:}")
    private String clientSecret;
    @Value("${pathao.username:}")
    private String username;
    @Value("${pathao.password:}")
    private String password;

    private final CourierAccountRepository accounts;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public PathaoClient(CourierAccountRepository accounts) {
        this.accounts = accounts;
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !username.isBlank() && !password.isBlank();
    }

    public boolean isSandbox() { return baseUrl.contains("sandbox"); }

    // ------------------------------------------------------------------ token handling

    /** Forces a fresh token exchange. Used by the "test connection" button. */
    public void testConnection() {
        accessToken(true);
    }

    private synchronized String accessToken(boolean force) {
        if (!isConfigured()) throw new PathaoApiException(400, "Pathao credentials are not configured on the server");

        CourierAccount acc = accounts.findByProvider(PROVIDER).orElseGet(() -> {
            CourierAccount a = new CourierAccount();
            a.setProvider(PROVIDER);
            a.setDisplayName("Pathao");
            a.setEnabled(false);
            return a;
        });

        boolean valid = acc.getAccessToken() != null && acc.getTokenExpiresAt() != null
                && acc.getTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(10));
        if (valid && !force) return acc.getAccessToken();

        JsonNode tokens = null;
        if (acc.getRefreshToken() != null) {
            try {
                tokens = tokenRequest(Map.of("client_id", clientId, "client_secret", clientSecret,
                        "grant_type", "refresh_token", "refresh_token", acc.getRefreshToken()));
            } catch (PathaoApiException ignored) {
                // refresh token expired/revoked - fall back to a full login below
            }
        }
        if (tokens == null) {
            tokens = tokenRequest(Map.of("client_id", clientId, "client_secret", clientSecret,
                    "grant_type", "password", "username", username, "password", password));
        }

        acc.setAccessToken(tokens.path("access_token").asText());
        if (tokens.hasNonNull("refresh_token")) acc.setRefreshToken(tokens.get("refresh_token").asText());
        acc.setTokenExpiresAt(LocalDateTime.now().plusSeconds(tokens.path("expires_in").asLong(432000)));
        acc.setSandbox(isSandbox());
        accounts.save(acc);
        return acc.getAccessToken();
    }

    private JsonNode tokenRequest(Map<String, Object> body) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<JsonNode> res = rest.postForEntity(baseUrl + API + "/issue-token",
                    new HttpEntity<>(body, h), JsonNode.class);
            JsonNode b = res.getBody();
            if (b == null || !b.hasNonNull("access_token")) throw new PathaoApiException(502, "Pathao returned no access token");
            return b;
        } catch (HttpStatusCodeException e) {
            throw toException(e);
        }
    }

    // ------------------------------------------------------------------ generic call

    private JsonNode call(HttpMethod method, String path, Object body) {
        try {
            return exchange(method, path, body, accessToken(false));
        } catch (PathaoApiException e) {
            if (e.getStatus() == 401) { // token revoked early - get a new one and retry once
                return exchange(method, path, body, accessToken(true));
            }
            throw e;
        }
    }

    private JsonNode exchange(HttpMethod method, String path, Object body, String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        try {
            ResponseEntity<JsonNode> res = rest.exchange(baseUrl + API + path, method, new HttpEntity<>(body, h), JsonNode.class);
            return res.getBody();
        } catch (HttpStatusCodeException e) {
            throw toException(e);
        } catch (PathaoApiException e) {
            throw e;
        } catch (Exception e) {
            throw new PathaoApiException(502, "Could not reach Pathao: " + e.getMessage());
        }
    }

    /** Pathao 422 bodies look like {message, errors:{field:[msg]}}; flatten them into one sentence. */
    private PathaoApiException toException(HttpStatusCodeException e) {
        String msg = "Pathao error " + e.getStatusCode().value();
        try {
            JsonNode b = mapper.readTree(e.getResponseBodyAsString());
            StringBuilder sb = new StringBuilder();
            if (b.hasNonNull("message")) sb.append(b.get("message").asText());
            JsonNode errors = b.get("errors");
            if (errors != null && errors.isObject()) {
                errors.fields().forEachRemaining(f -> {
                    sb.append(sb.length() > 0 ? "; " : "").append(f.getKey()).append(": ");
                    sb.append(f.getValue().isArray() && f.getValue().size() > 0 ? f.getValue().get(0).asText() : f.getValue().asText());
                });
            }
            if (sb.length() > 0) msg = sb.toString();
        } catch (Exception ignored) { /* keep generic message */ }
        return new PathaoApiException(e.getStatusCode().value(), msg);
    }

    // ------------------------------------------------------------------ endpoints

    /** GET /stores -> array of stores. */
    public JsonNode listStores() {
        return call(HttpMethod.GET, "/stores", null).path("data").path("data");
    }

    public JsonNode createStore(Map<String, Object> payload) {
        return call(HttpMethod.POST, "/stores", payload);
    }

    public JsonNode listCities() {
        return call(HttpMethod.GET, "/city-list", null).path("data").path("data");
    }

    public JsonNode listZones(long cityId) {
        return call(HttpMethod.GET, "/cities/" + cityId + "/zone-list", null).path("data").path("data");
    }

    public JsonNode listAreas(long zoneId) {
        return call(HttpMethod.GET, "/zones/" + zoneId + "/area-list", null).path("data").path("data");
    }

    /** POST /merchant/price-plan -> {price, discount, cod_percentage, final_price, ...} */
    public JsonNode priceQuote(Map<String, Object> payload) {
        return call(HttpMethod.POST, "/merchant/price-plan", payload).path("data");
    }

    /** POST /orders -> {consignment_id, merchant_order_id, order_status, delivery_fee} */
    public JsonNode createOrder(Map<String, Object> payload) {
        return call(HttpMethod.POST, "/orders", payload).path("data");
    }

    /** POST /orders/bulk - asynchronous (HTTP 202, no consignment ids). Not used for booking yet; see roadmap step 6. */
    public JsonNode createBulk(java.util.List<Map<String, Object>> orders) {
        Map<String, Object> body = new HashMap<>();
        body.put("orders", orders);
        return call(HttpMethod.POST, "/orders/bulk", body);
    }

    /** GET /orders/{consignment}/info -> {consignment_id, merchant_order_id, order_status, order_status_slug, updated_at} */
    public JsonNode orderInfo(String consignmentId) {
        return call(HttpMethod.GET, "/orders/" + consignmentId + "/info", null).path("data");
    }
}
