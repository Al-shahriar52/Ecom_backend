package ecommerce.controller.impl;

import ecommerce.controller.FrequentlyBoughtController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.frequentlyBought.FrequentlyBoughtItemDto;
import ecommerce.dto.frequentlyBought.FrequentlyBoughtRequestDto;
import ecommerce.service.FrequentlyBoughtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/frequently-bought")
@RequiredArgsConstructor
public class FrequentlyBoughtControllerImpl implements FrequentlyBoughtController {

    private final FrequentlyBoughtService frequentlyBoughtService;
    @Override
    @PostMapping("/together")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<?> frequentlyBoughtTogether(@Valid @RequestBody FrequentlyBoughtRequestDto frequentlyBoughtRequestDto) {
        FrequentlyBoughtRequestDto response = frequentlyBoughtService.frequentlyBoughtTogether(frequentlyBoughtRequestDto);
        return new ResponseEntity<>(GenericResponseDto.success("Frequently bought together updated successfully", response, HttpStatus.OK.value()),  HttpStatus.OK);
    }

    @GetMapping("/get/product/{productId}")
    public ResponseEntity<?> getFbtProducts(@PathVariable Long productId) {
        List<FrequentlyBoughtItemDto> pairedProducts = frequentlyBoughtService.getFrequentlyBoughtTogetherProducts(productId);
        return new ResponseEntity<>(GenericResponseDto.success("Frequently bought together products fetched.", pairedProducts, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @DeleteMapping("/together")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unpairProducts(@RequestBody FrequentlyBoughtRequestDto requestDto) {
        frequentlyBoughtService.unpairFrequentlyBoughtProduct(requestDto.getMainProductId(), requestDto.getPairedProductId());
        return ResponseEntity.ok(GenericResponseDto.success("Product pairing removed successfully.", null, HttpStatus.OK.value()));
    }

}
