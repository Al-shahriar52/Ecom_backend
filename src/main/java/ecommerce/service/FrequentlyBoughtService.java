package ecommerce.service;

import ecommerce.dto.frequentlyBought.FrequentlyBoughtItemDto;
import ecommerce.dto.frequentlyBought.FrequentlyBoughtRequestDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FrequentlyBoughtService {
    FrequentlyBoughtRequestDto frequentlyBoughtTogether(FrequentlyBoughtRequestDto frequentlyBoughtRequestDto);
    List<FrequentlyBoughtItemDto> getFrequentlyBoughtTogetherProducts(Long productId);

    void unpairFrequentlyBoughtProduct(Long mainProductId, Long pairedProductId);
}
