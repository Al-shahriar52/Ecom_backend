package ecommerce.service.impl;

import ecommerce.entity.*;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.*;
import ecommerce.utils.ImageUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/** Admin CRUD for brands, categories, sub-categories, cities and areas. */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private final BrandRepository brandRepo;
    private final CategoryRepository categoryRepo;
    private final SubCategoryRepository subCategoryRepo;
    private final CitiesRepository cityRepo;
    private final AreasRepository areaRepo;
    private final CatalogCountRepository counts;
    private final ImageUtil imageUtil;

    // ------------------------------------------------------------ helpers

    private static String name(String raw, String what) {
        String n = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (n.isEmpty()) throw new BadRequestException("Please enter the " + what + " name.");
        if (n.length() > 100) throw new BadRequestException("The " + what + " name is too long (100 characters max).");
        return n;
    }

    /** Same rule as the storefront's slugify(), so /brand/<slug> links keep matching. */
    static String slug(String name) {
        return name.toLowerCase().replace(" & ", "-").replace(" ", "-");
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] r : rows) m.put(((Number) r[0]).longValue(), ((Number) r[1]).longValue());
        return m;
    }

    private static void checkImage(MultipartFile f) {
        String ct = f.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("image/")) throw new BadRequestException("Only image files can be uploaded.");
        if (f.getSize() > 5L * 1024 * 1024) throw new BadRequestException("Image is too large (5 MB max).");
    }

    private static Map<String, Object> row(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    // ------------------------------------------------------------ brands

    public List<Map<String, Object>> brands() {
        Map<Long, Long> c = toMap(counts.brandCounts());
        return brandRepo.findAll().stream()
                .sorted(Comparator.comparing(b -> String.valueOf(b.getName()).toLowerCase()))
                .map(b -> row("id", b.getId(), "name", b.getName(), "slug", b.getSlug(), "logoUrl", b.getLogoUrl(),
                        "top", b.isTop(), "productCount", c.getOrDefault(b.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> saveBrand(Long id, String rawName, Boolean top, MultipartFile logo, boolean removeLogo) throws IOException {
        String n = name(rawName, "brand");
        for (Brand o : brandRepo.findAll()) {
            if (!o.getId().equals(id) && o.getName() != null && o.getName().equalsIgnoreCase(n))
                throw new BadRequestException("A brand called \"" + o.getName() + "\" already exists.");
        }
        Brand b = id == null ? new Brand() : brandRepo.findById(id).orElseThrow(() -> new BadRequestException("Brand not found."));
        b.setName(n);
        b.setSlug(slug(n));
        if (top != null) b.setTop(top);
        if (logo != null && !logo.isEmpty()) { checkImage(logo); b.setLogoUrl(imageUtil.uploadFile(logo)); }
        else if (removeLogo) b.setLogoUrl(null);
        b = brandRepo.save(b);
        return row("id", b.getId(), "name", b.getName());
    }

    @Transactional
    public void deleteBrand(Long id) {
        Brand b = brandRepo.findById(id).orElseThrow(() -> new BadRequestException("Brand not found."));
        long n = counts.countBrand(id);
        if (n > 0) throw new BadRequestException("\"" + b.getName() + "\" is used by " + n + " product" + (n == 1 ? "" : "s") + ". Move or delete those products first.");
        brandRepo.delete(b);
    }

    // ------------------------------------------------------------ categories

    public List<Map<String, Object>> categories() {
        Map<Long, Long> c = toMap(counts.categoryCounts());
        Map<Long, Long> subs = subCategoryRepo.findAll().stream()
                .filter(s -> s.getCategory() != null)
                .collect(Collectors.groupingBy(s -> s.getCategory().getId(), Collectors.counting()));
        return categoryRepo.findAll().stream()
                .sorted(Comparator.comparing(x -> String.valueOf(x.getName()).toLowerCase()))
                .map(x -> row("id", x.getId(), "name", x.getName(), "iconUrl", x.getIconUrl(),
                        "productCount", c.getOrDefault(x.getId(), 0L), "subCategoryCount", subs.getOrDefault(x.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> saveCategory(Long id, String rawName, MultipartFile icon, boolean removeIcon) throws IOException {
        String n = name(rawName, "category");
        for (Category o : categoryRepo.findAll()) {
            if (!o.getId().equals(id) && o.getName() != null && o.getName().equalsIgnoreCase(n))
                throw new BadRequestException("A category called \"" + o.getName() + "\" already exists.");
        }
        Category c = id == null ? new Category() : categoryRepo.findById(id).orElseThrow(() -> new BadRequestException("Category not found."));
        c.setName(n);
        if (icon != null && !icon.isEmpty()) { checkImage(icon); c.setIconUrl(imageUtil.uploadFile(icon)); }
        else if (removeIcon) c.setIconUrl(null);
        c = categoryRepo.save(c);
        return row("id", c.getId(), "name", c.getName());
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category c = categoryRepo.findById(id).orElseThrow(() -> new BadRequestException("Category not found."));
        long subs = subCategoryRepo.findAll().stream().filter(s -> s.getCategory() != null && id.equals(s.getCategory().getId())).count();
        if (subs > 0) throw new BadRequestException("\"" + c.getName() + "\" still has " + subs + " sub-categor" + (subs == 1 ? "y" : "ies") + ". Delete those first.");
        long n = counts.countCategory(id);
        if (n > 0) throw new BadRequestException("\"" + c.getName() + "\" is used by " + n + " product" + (n == 1 ? "" : "s") + ".");
        categoryRepo.delete(c);
    }

    // ------------------------------------------------------------ sub-categories

    public List<Map<String, Object>> subCategories() {
        Map<Long, Long> c = toMap(counts.subCategoryCounts());
        return subCategoryRepo.findAll().stream()
                .sorted(Comparator.comparing((SubCategory s) -> s.getCategory() == null ? "" : String.valueOf(s.getCategory().getName()).toLowerCase())
                        .thenComparing(s -> String.valueOf(s.getName()).toLowerCase()))
                .map(s -> row("id", s.getId(), "name", s.getName(),
                        "categoryId", s.getCategory() == null ? null : s.getCategory().getId(),
                        "categoryName", s.getCategory() == null ? null : s.getCategory().getName(),
                        "productCount", c.getOrDefault(s.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> saveSubCategory(Long id, String rawName, Long categoryId) {
        String n = name(rawName, "sub-category");
        if (categoryId == null) throw new BadRequestException("Choose the parent category.");
        Category cat = categoryRepo.findById(categoryId).orElseThrow(() -> new BadRequestException("Category not found."));
        for (SubCategory o : subCategoryRepo.findAll()) {
            if (!o.getId().equals(id) && o.getName() != null && o.getName().equalsIgnoreCase(n)
                    && o.getCategory() != null && categoryId.equals(o.getCategory().getId()))
                throw new BadRequestException("\"" + cat.getName() + "\" already has a sub-category called \"" + o.getName() + "\".");
        }
        SubCategory s = id == null ? new SubCategory() : subCategoryRepo.findById(id).orElseThrow(() -> new BadRequestException("Sub-category not found."));
        s.setName(n);
        s.setCategory(cat);
        s = subCategoryRepo.save(s);
        return row("id", s.getId(), "name", s.getName());
    }

    @Transactional
    public void deleteSubCategory(Long id) {
        SubCategory s = subCategoryRepo.findById(id).orElseThrow(() -> new BadRequestException("Sub-category not found."));
        long n = counts.countSubCategory(id);
        if (n > 0) throw new BadRequestException("\"" + s.getName() + "\" is used by " + n + " product" + (n == 1 ? "" : "s") + ". Move or delete those products first.");
        subCategoryRepo.delete(s);
    }

    // ------------------------------------------------------------ cities

    public List<Map<String, Object>> cities() {
        Map<Long, Long> areaCount = areaRepo.findAll().stream()
                .filter(a -> a.getCityId() != null)
                .collect(Collectors.groupingBy(Areas::getCityId, Collectors.counting()));
        return cityRepo.findAll().stream()
                .sorted(Comparator.comparing(x -> String.valueOf(x.getName()).toLowerCase()))
                .map(x -> row("id", x.getId(), "name", x.getName(), "areaCount", areaCount.getOrDefault(x.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> saveCity(Long id, String rawName) {
        String n = name(rawName, "city");
        for (Cities o : cityRepo.findAll()) {
            if (!o.getId().equals(id) && o.getName() != null && o.getName().equalsIgnoreCase(n))
                throw new BadRequestException("A city called \"" + o.getName() + "\" already exists.");
        }
        Cities c = id == null ? new Cities() : cityRepo.findById(id).orElseThrow(() -> new BadRequestException("City not found."));
        c.setName(n);
        c = cityRepo.save(c);
        return row("id", c.getId(), "name", c.getName());
    }

    @Transactional
    public void deleteCity(Long id) {
        Cities c = cityRepo.findById(id).orElseThrow(() -> new BadRequestException("City not found."));
        long areas = areaRepo.findAll().stream().filter(a -> id.equals(a.getCityId())).count();
        if (areas > 0) throw new BadRequestException("\"" + c.getName() + "\" still has " + areas + " area" + (areas == 1 ? "" : "s") + ". Delete the areas first.");
        cityRepo.delete(c);
    }

    // ------------------------------------------------------------ areas

    public List<Map<String, Object>> areas(Long cityId) {
        Map<Long, String> cityNames = cityRepo.findAll().stream().collect(Collectors.toMap(Cities::getId, x -> String.valueOf(x.getName())));
        return areaRepo.findAll().stream()
                .filter(a -> cityId == null || cityId.equals(a.getCityId()))
                .sorted(Comparator.comparing((Areas a) -> cityNames.getOrDefault(a.getCityId(), "").toLowerCase())
                        .thenComparing(a -> String.valueOf(a.getName()).toLowerCase()))
                .map(a -> row("id", a.getId(), "name", a.getName(), "cityId", a.getCityId(), "cityName", cityNames.get(a.getCityId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> saveArea(Long id, String rawName, Long cityId) {
        String n = name(rawName, "area");
        if (cityId == null || !cityRepo.existsById(cityId)) throw new BadRequestException("Choose a city.");
        for (Areas o : areaRepo.findAll()) {
            if (!o.getId().equals(id) && o.getName() != null && o.getName().equalsIgnoreCase(n) && cityId.equals(o.getCityId()))
                throw new BadRequestException("This city already has an area called \"" + o.getName() + "\".");
        }
        Areas a = id == null ? new Areas() : areaRepo.findById(id).orElseThrow(() -> new BadRequestException("Area not found."));
        a.setName(n);
        a.setCityId(cityId);
        a = areaRepo.save(a);
        return row("id", a.getId(), "name", a.getName());
    }

    /** Many areas at once, one per line (or comma separated). Existing names are skipped. */
    @Transactional
    public Map<String, Object> bulkAreas(Long cityId, String names) {
        if (cityId == null || !cityRepo.existsById(cityId)) throw new BadRequestException("Choose a city.");
        Set<String> have = areaRepo.findAll().stream().filter(a -> cityId.equals(a.getCityId()) && a.getName() != null)
                .map(a -> a.getName().toLowerCase()).collect(Collectors.toSet());
        int added = 0, skipped = 0;
        for (String part : (names == null ? "" : names).split("[\\n,]")) {
            String n = part.trim().replaceAll("\\s+", " ");
            if (n.isEmpty()) continue;
            if (n.length() > 100 || !have.add(n.toLowerCase())) { skipped++; continue; }
            Areas a = new Areas();
            a.setName(n);
            a.setCityId(cityId);
            areaRepo.save(a);
            added++;
        }
        if (added == 0 && skipped == 0) throw new BadRequestException("Type at least one area name.");
        return row("added", added, "skipped", skipped);
    }

    @Transactional
    public void deleteArea(Long id) {
        Areas a = areaRepo.findById(id).orElseThrow(() -> new BadRequestException("Area not found."));
        areaRepo.delete(a);
    }
}
