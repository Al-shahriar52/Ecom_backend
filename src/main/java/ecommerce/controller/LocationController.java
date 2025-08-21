package ecommerce.controller;

import org.springframework.http.ResponseEntity;

public interface LocationController {

    ResponseEntity<?> cities();
    ResponseEntity<?> areas(Long cityId);
}
