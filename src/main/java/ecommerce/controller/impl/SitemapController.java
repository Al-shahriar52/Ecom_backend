package ecommerce.controller.impl;

import ecommerce.entity.Brand;
import ecommerce.entity.Category;
import ecommerce.entity.Product;
import ecommerce.entity.ProductStatus;
import ecommerce.repository.BrandRepository;
import ecommerce.repository.CategoryRepository;
import ecommerce.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Public sitemap for Google.
 *
 * SEO notes:
 *  - Only PUBLISHED products are listed (DRAFT products are hidden from the storefront).
 *  - <lastmod> is the real product update date. A sitemap where every URL says "today"
 *    teaches Google to ignore lastmod. <changefreq>/<priority> are ignored by Google, so omitted.
 *  - Category/brand URLs are built from the database with the SAME slug rule as the frontend
 *    (src/utils/slugify.js), so every URL listed here really renders a page.
 *  - Served as UTF-8.
 */
@RestController
public class SitemapController {

    private static final String BASE_URL = "https://beautyhaat.com";

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public SitemapController(ProductService productService,
                             CategoryRepository categoryRepository,
                             BrandRepository brandRepository) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    public String getSitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. Static pages
        xml.append(node(BASE_URL + "/", null));
        xml.append(node(BASE_URL + "/shop", null));
        xml.append(node(BASE_URL + "/brands", null));
        xml.append(node(BASE_URL + "/about", null));
        xml.append(node(BASE_URL + "/contact", null));

        // 2. Categories (from the database, same slug rule as the frontend)
        Set<String> categorySlugs = new LinkedHashSet<>();
        for (Category category : categoryRepository.findAll()) {
            String slug = slugify(category.getName());
            if (!slug.isEmpty()) {
                categorySlugs.add(slug);
            }
        }
        for (String slug : categorySlugs) {
            xml.append(node(BASE_URL + "/category/" + slug, null));
        }

        // 3. Brands
        Set<String> brandSlugs = new LinkedHashSet<>();
        for (Brand brand : brandRepository.findAll()) {
            String slug = slugify(brand.getName());
            if (!slug.isEmpty()) {
                brandSlugs.add(slug);
            }
        }
        for (String slug : brandSlugs) {
            xml.append(node(BASE_URL + "/brand/" + slug, null));
        }

        // 4. Products (published only)
        List<Product> allProducts = productService.findAll();
        for (Product product : allProducts) {
            if (product.getSlug() == null || product.getSlug().isBlank()) {
                continue;
            }
            if (product.getStatus() == ProductStatus.DRAFT) {
                continue;
            }
            LocalDateTime updated = product.getUpdatedAt() != null ? product.getUpdatedAt() : product.getCreatedAt();
            String lastMod = updated != null ? updated.toLocalDate().toString() : null;
            xml.append(node(BASE_URL + "/product/" + product.getSlug(), lastMod));
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    /** Same rule as frontend src/utils/slugify.js: lowercase, " & " -> "-", " " -> "-". */
    private static String slugify(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase().replace(" & ", "-").replace(" ", "-").trim();
    }

    private String node(String url, String lastMod) {
        String safeUrl = url.replace("&", "&amp;")
                .replace("'", "&apos;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        return "  <url>\n" +
                "    <loc>" + safeUrl + "</loc>\n" +
                (lastMod != null ? "    <lastmod>" + lastMod + "</lastmod>\n" : "") +
                "  </url>\n";
    }
}
