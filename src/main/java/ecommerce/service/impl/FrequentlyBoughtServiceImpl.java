package ecommerce.service.impl;

import ecommerce.dto.frequentlyBought.FrequentlyBoughtItemDto;
import ecommerce.dto.frequentlyBought.FrequentlyBoughtRequestDto;
import ecommerce.entity.Product;
import ecommerce.entity.ProductImage;
import ecommerce.exceptionHandling.ResourceNotFoundException;
import ecommerce.repository.ProductImageRepository;
import ecommerce.repository.ProductRepository;
import ecommerce.service.FrequentlyBoughtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FrequentlyBoughtServiceImpl implements FrequentlyBoughtService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    @Override
    public FrequentlyBoughtRequestDto frequentlyBoughtTogether(FrequentlyBoughtRequestDto requestDto) {

        // Step 1: Fetch both the main product and the product to be paired.
        Product mainProduct = productRepository.findById(requestDto.getMainProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Main product not found with id: " + requestDto.getMainProductId()));

        Product pairedProduct = productRepository.findById(requestDto.getPairedProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Paired product not found with id: " + requestDto.getPairedProductId()));

        // Step 2: Get the main product's set of paired items and add the new one.
        mainProduct.getFrequentlyBoughtTogether().add(pairedProduct);

        // Step 3: Save the main product. JPA/Hibernate will automatically update
        // the 'product_frequently_bought_together' join table with the new pairing.
        productRepository.save(mainProduct);
        return requestDto;
    }

    @Override
    public List<FrequentlyBoughtItemDto> getFrequentlyBoughtTogetherProducts(Long productId) {
        Product mainProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Get the set of paired products and convert them to DTOs
        return mainProduct.getFrequentlyBoughtTogether().stream()
                .map(this::convertToFrequentlyBoughtItemDto) // Assuming you have a helper method for conversion
                .collect(Collectors.toList());
    }

    // Helper method to convert Product entity to ProductCardDto
    private FrequentlyBoughtItemDto convertToFrequentlyBoughtItemDto(Product product) {
        List<ProductImage> imageUrlsByProductId = productImageRepository.findImageUrlsByProductId(product.getId());
        FrequentlyBoughtItemDto dto = new FrequentlyBoughtItemDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setImageUrl(imageUrlsByProductId.get(0).getImageUrl());
        dto.setDiscountedPrice(product.getDiscountedPrice());
        return dto;
    }

    @Transactional // This is important for database operations
    public void unpairFrequentlyBoughtProduct(Long mainProductId, Long pairedProductId) {
        // 1. Fetch the main product
        Product mainProduct = productRepository.findById(mainProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Main product not found with id: " + mainProductId));

        // 2. Find the specific paired product to remove from the set
        // This is more efficient than fetching the whole entity if you only need to remove the link
        mainProduct.getFrequentlyBoughtTogether()
                .removeIf(pairedProd -> pairedProd.getId().equals(pairedProductId));

        // 3. Save the main product. JPA/Hibernate will automatically delete the corresponding
        // row from the 'product_frequently_bought_together' join table.
        productRepository.save(mainProduct);
    }
}
