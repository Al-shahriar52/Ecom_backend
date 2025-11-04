package ecommerce.controller;

import ecommerce.dto.frequentlyBought.FrequentlyBoughtRequestDto;
import org.springframework.http.ResponseEntity;

public interface FrequentlyBoughtController {

    ResponseEntity<?> frequentlyBoughtTogether(FrequentlyBoughtRequestDto frequentlyBoughtRequestDto);
}
