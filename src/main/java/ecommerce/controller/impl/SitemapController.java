package ecommerce.controller.impl;

import ecommerce.entity.Product;
import ecommerce.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SitemapController {

    private final ProductService productService;

    public SitemapController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String getSitemap() {
        StringBuilder xml = new StringBuilder();
        String today = java.time.LocalDate.now().toString();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        // FIX: Corrected the namespace URL
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. Home Page
        xml.append(createSitemapNode("https://beautyhaat.com/", today, "1.0"));

        // 2. Categories
        String[] categories = {
                "makeup", "skin", "hair", "personal-care", "mom-baby",
                "fragrance", "undergarments", "combo", "jewellery", "clearance-sale", "men"
        };

        for (String cat : categories) {
            // Ensure no spaces in the URL
            String safeCat = cat.toLowerCase().replace(" ", "-");
            xml.append(createSitemapNode("https://beautyhaat.com/category/" + safeCat, today, "0.8"));
        }

        // 3. Products
        List<Product> allProducts = productService.findAll();
        for (Product product : allProducts) {
            String productUrl = "https://beautyhaat.com/product/" + product.getId();
            xml.append(createSitemapNode(productUrl, today, "0.9"));
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private String createSitemapNode(String url, String lastMod, String priority) {
        // More robust escaping for XML safety
        String safeUrl = url.replace("&", "&amp;")
                .replace("'", "&apos;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        return "  <url>\n" +
                "    <loc>" + safeUrl + "</loc>\n" +
                "    <lastmod>" + lastMod + "</lastmod>\n" +
                "    <changefreq>daily</changefreq>\n" + // Changed to daily for e-commerce
                "    <priority>" + priority + "</priority>\n" +
                "  </url>\n";
    }
}