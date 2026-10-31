package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.service.impl.AdminCatalogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/** Admin: brands, categories, sub-categories, cities, areas. */
@RestController
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("@perm.has(authentication, 13)")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService service;

    private static ResponseEntity<?> ok(String msg, Object data) {
        return ResponseEntity.ok(GenericResponseDto.success(msg, data, HttpStatus.OK.value()));
    }

    private static Long asLong(Object o) {
        if (o == null || o.toString().isBlank()) return null;
        return Long.valueOf(o.toString());
    }

    // ---- brands
    @GetMapping("/brands")
    public ResponseEntity<?> brands() { return ok("Brands", service.brands()); }

    @PostMapping(value = "/brands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addBrand(@RequestParam String name, @RequestParam(required = false) Boolean top,
                                      @RequestPart(required = false) MultipartFile logo) throws IOException {
        return ok("Brand added", service.saveBrand(null, name, top, logo, false));
    }

    @PutMapping(value = "/brands/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editBrand(@PathVariable Long id, @RequestParam String name, @RequestParam(required = false) Boolean top,
                                       @RequestParam(required = false, defaultValue = "false") boolean removeLogo,
                                       @RequestPart(required = false) MultipartFile logo) throws IOException {
        return ok("Brand saved", service.saveBrand(id, name, top, logo, removeLogo));
    }

    @DeleteMapping("/brands/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable Long id) { service.deleteBrand(id); return ok("Brand deleted", null); }

    // ---- categories
    @GetMapping("/categories")
    public ResponseEntity<?> categories() { return ok("Categories", service.categories()); }

    @PostMapping(value = "/categories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addCategory(@RequestParam String name, @RequestPart(required = false) MultipartFile icon) throws IOException {
        return ok("Category added", service.saveCategory(null, name, icon, false));
    }

    @PutMapping(value = "/categories/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editCategory(@PathVariable Long id, @RequestParam String name,
                                          @RequestParam(required = false, defaultValue = "false") boolean removeIcon,
                                          @RequestPart(required = false) MultipartFile icon) throws IOException {
        return ok("Category saved", service.saveCategory(id, name, icon, removeIcon));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) { service.deleteCategory(id); return ok("Category deleted", null); }

    // ---- sub-categories
    @GetMapping("/sub-categories")
    public ResponseEntity<?> subCategories() { return ok("Sub-categories", service.subCategories()); }

    @PostMapping("/sub-categories")
    public ResponseEntity<?> addSub(@RequestBody Map<String, Object> b) {
        return ok("Sub-category added", service.saveSubCategory(null, (String) b.get("name"), asLong(b.get("categoryId"))));
    }

    @PutMapping("/sub-categories/{id}")
    public ResponseEntity<?> editSub(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        return ok("Sub-category saved", service.saveSubCategory(id, (String) b.get("name"), asLong(b.get("categoryId"))));
    }

    @DeleteMapping("/sub-categories/{id}")
    public ResponseEntity<?> deleteSub(@PathVariable Long id) { service.deleteSubCategory(id); return ok("Sub-category deleted", null); }

    // ---- cities
    @GetMapping("/cities")
    public ResponseEntity<?> cities() { return ok("Cities", service.cities()); }

    @PostMapping("/cities")
    public ResponseEntity<?> addCity(@RequestBody Map<String, Object> b) { return ok("City added", service.saveCity(null, (String) b.get("name"))); }

    @PutMapping("/cities/{id}")
    public ResponseEntity<?> editCity(@PathVariable Long id, @RequestBody Map<String, Object> b) { return ok("City saved", service.saveCity(id, (String) b.get("name"))); }

    @DeleteMapping("/cities/{id}")
    public ResponseEntity<?> deleteCity(@PathVariable Long id) { service.deleteCity(id); return ok("City deleted", null); }

    // ---- areas
    @GetMapping("/areas")
    public ResponseEntity<?> areas(@RequestParam(required = false) Long cityId) { return ok("Areas", service.areas(cityId)); }

    @PostMapping("/areas")
    public ResponseEntity<?> addArea(@RequestBody Map<String, Object> b) {
        return ok("Area added", service.saveArea(null, (String) b.get("name"), asLong(b.get("cityId"))));
    }

    @PostMapping("/areas/bulk")
    public ResponseEntity<?> bulkAreas(@RequestBody Map<String, Object> b) {
        return ok("Areas added", service.bulkAreas(asLong(b.get("cityId")), (String) b.get("names")));
    }

    @PutMapping("/areas/{id}")
    public ResponseEntity<?> editArea(@PathVariable Long id, @RequestBody Map<String, Object> b) {
        return ok("Area saved", service.saveArea(id, (String) b.get("name"), asLong(b.get("cityId"))));
    }

    @DeleteMapping("/areas/{id}")
    public ResponseEntity<?> deleteArea(@PathVariable Long id) { service.deleteArea(id); return ok("Area deleted", null); }
}
