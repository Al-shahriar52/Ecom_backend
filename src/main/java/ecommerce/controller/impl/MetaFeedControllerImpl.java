package ecommerce.controller.impl;

import ecommerce.controller.MetaFeedController;
import ecommerce.entity.Product;
import ecommerce.repository.ProductRepository; // Assuming you have standard JpaRepository
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class MetaFeedControllerImpl implements MetaFeedController {

    private final ProductRepository productRepository;

    public MetaFeedControllerImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping(value = "/meta-feed.csv", produces = "text/csv")
    @Transactional(readOnly = true) // Required because your imageUrls list is fetched Lazily
    public void generateMetaFeed(HttpServletResponse response) throws Exception {
        // Force the browser/Meta scraper to download this as a CSV file
        response.setHeader("Content-Disposition", "attachment; filename=\"meta-feed.csv\"");
        response.setContentType("text/csv; charset=UTF-8");

        PrintWriter writer = response.getWriter();
        
        // 1. Meta's strictly required header row
        writer.println("id,title,description,availability,condition,price,link,image_link,brand,sale_price");

        // 2. Fetch all products (In production, you may want to filter by status)
        List<Product> products = productRepository.findAll();

        for (Product product : products) {
            // Map Entity fields to Meta CSV specifications
            String id = escapeCsv(product.getSku(), 100);
            String title = escapeCsv(product.getName(), 150);
            String description = escapeCsv(product.getDescription(), 9999); 
            
            // Meta requires specific lowercase availability strings
            String availability = product.getQuantity() > 0 ? "in stock" : "out of stock";
            String condition = "new"; 
            
            // Prices MUST include the ISO currency code (Meta will reject it without "BDT")
            String price = product.getOriginalPrice() + " BDT";
            String salePrice = "";
            if (product.getDiscountedPrice() > 0 && product.getDiscountedPrice() < product.getOriginalPrice()) {
                salePrice = product.getDiscountedPrice() + " BDT";
            }

            String link = escapeCsv("https://beautyhaat.com/product/" + product.getSlug(), 2000);
            
            String imageLink = "";
            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                imageLink = escapeCsv(product.getImageUrls().get(0).getImageUrl(), 2000);
            }

            // Fallback to BeautyHaat if the brand is missing
            String brandName = product.getBrand() != null ? product.getBrand().getName() : "BeautyHaat";
            String brand = escapeCsv(brandName, 100);

            // 3. Write row
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    id, title, description, availability, condition, price, link, imageLink, brand, salePrice);
        }

        writer.flush();
        writer.close();
    }

    /**
     * Helper method to clean HTML, enforce Meta's character limits, 
     * and properly escape CSV commas/quotes so the feed doesn't break.
     */
    private String escapeCsv(String value, int maxLength) {
        if (value == null) return "";
        
        // Remove HTML tags (Meta requires plain text for descriptions)
        String plainText = value.replaceAll("<[^>]*>", "").trim();
        
        if (plainText.length() > maxLength) {
            plainText = plainText.substring(0, maxLength);
        }
        
        // Escape double quotes by doubling them (Standard CSV rule)
        String escaped = plainText.replace("\"", "\"\"");
        
        // Wrap the whole string in quotes if it contains commas, newlines, or quotes
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}