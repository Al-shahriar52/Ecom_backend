package ecommerce.controller.impl;

import ecommerce.controller.ShippingController;
import ecommerce.dto.ShippingDto;
import ecommerce.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shipping")
public class ShippingControllerImpl implements ShippingController {

    private final ShippingService shippingService;

    public ShippingControllerImpl(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @Override
    @PostMapping("/add")
    public ResponseEntity<?> add(@Valid @RequestBody ShippingDto shippingDto) {

        ShippingDto shipping = shippingService.add(shippingDto);
        return new ResponseEntity<>(shipping, HttpStatus.CREATED);
    }

    @Override
    @PutMapping("/update/{shippingId}")
    public ResponseEntity<?> update(@Valid @RequestBody ShippingDto shippingDto,
                                    @PathVariable Long shippingId) {

        ShippingDto response = shippingService.update(shippingDto, shippingId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
