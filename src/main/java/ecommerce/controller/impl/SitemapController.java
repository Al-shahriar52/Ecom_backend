package ecommerce.controller.impl;

import ecommerce.entity.Product;
import ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
// Import your Product service or repository here
// import com.beautyhaat.backend.service.ProductService; 
// import com.beautyhaat.backend.model.Product;

import java.util.List;

@RestController
public class SitemapController {

    private final ProductService productService;

    // Inject your existing ProductService
    public SitemapController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemap() {
        StringBuilder xml = new StringBuilder();

        // 1. Add XML headers
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 2. Add your static core pages
        xml.append(createSitemapNode("https://beautyhaat.com/", "1.0"));
        xml.append(createSitemapNode("https://beautyhaat.com/brand/null", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/makeup", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/skin", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/hair", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/personal-care", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/mom-baby", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/fragrance", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/undergarments", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/combo", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/jewellery", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/clearance-sale", "0.8"));
        xml.append(createSitemapNode("https://beautyhaat.com/category/men", "0.8"));

        // 3. Fetch all products using your exact method
        List<Product> allProducts = productService.findAll();

        // 4. Loop through your entity data
        for (Product product : allProducts) {

            // Optional but recommended: Only add the product if its status is active/published
            // if (product.getStatus() == ProductStatus.ACTIVE) {

            // Get the ID directly from your Product entity
            String productUrl = "https://beautyhaat.com/product/" + product.getId();
            xml.append(createSitemapNode(productUrl, "0.9"));

            // }
        }

        // 5. Close the sitemap tag
        xml.append("</urlset>");

        return xml.toString();
    }

    /**
     * Helper method to format the XML tags for each URL.
     */
    private String createSitemapNode(String url, String priority) {
        return "  <url>\n" +
                "    <loc>" + url + "</loc>\n" +
                "    <priority>" + priority + "</priority>\n" +
                "  </url>\n";
    }
}