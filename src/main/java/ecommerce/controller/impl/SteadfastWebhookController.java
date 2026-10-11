package ecommerce.controller.impl;

import ecommerce.service.impl.SteadfastWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public endpoint for Steadfast. Not behind JWT: it is protected by the bearer token and the body signature. */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class SteadfastWebhookController {

    private final SteadfastWebhookService service;

    @PostMapping("/steadfast")
    public ResponseEntity<String> receive(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody byte[] body) {
        return switch (service.handle(authorization, signature, idempotencyKey, body)) {
            case OK, DUPLICATE -> ResponseEntity.ok("ok");
            case UNAUTHORIZED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
            case NOT_CONFIGURED -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("not configured");
        };
    }
}
