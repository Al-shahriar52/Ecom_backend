package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.enums.BannerPlacement;
import ecommerce.service.impl.HomeContentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Home page content.
 *  - GET /api/v1/home/content                      public  (add "/api/v1/home/**" to SecurityConfig permitAll)
 *  - /api/v1/admin/home/banners/...                admin only
 */
@RestController
@RequiredArgsConstructor
public class HomeContentController {

    private final HomeContentService service;

    @GetMapping("/api/v1/home/content")
    public ResponseEntity<?> publicContent() {
        return ResponseEntity.ok(GenericResponseDto.success("Home content", service.publicContent(), HttpStatus.OK.value()));
    }

    @GetMapping("/api/v1/home/concern-products")
    public ResponseEntity<?> concernProducts(@RequestParam String keywords,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(GenericResponseDto.success("Products", service.concernProducts(keywords, page, size), HttpStatus.OK.value()));
    }

    @GetMapping("/api/v1/admin/home/banners")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> list(@RequestParam(required = false) BannerPlacement placement) {
        return ResponseEntity.ok(GenericResponseDto.success("Banners", service.list(placement), HttpStatus.OK.value()));
    }

    @PostMapping(value = "/api/v1/admin/home/banners", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> create(@RequestParam BannerPlacement placement,
                                    @RequestParam(required = false) String title,
                                    @RequestParam(required = false) String subtitle,
                                    @RequestParam(required = false) String buttonText,
                                    @RequestParam(required = false) String linkUrl,
                                    @RequestParam(required = false) String badgeText,
                                    @RequestParam(required = false) String keywords,
                                    @RequestParam(required = false) String bgColor,
                                    @RequestParam(required = false) String textColor,
                                    @RequestParam(required = false) String textAlign,
                                    @RequestParam(required = false) String startsAt,
                                    @RequestParam(required = false) String endsAt,
                                    @RequestParam(required = false) Boolean showCountdown,
                                    @RequestParam(required = false) Boolean active,
                                    @RequestPart(required = false) MultipartFile image,
                                    @RequestPart(required = false) MultipartFile mobileImage) throws IOException {
        var form = new HomeContentService.Form(placement, title, subtitle, buttonText, linkUrl, badgeText, bgColor, textColor,
                textAlign, startsAt, endsAt, showCountdown, active, null, keywords);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponseDto.success("Banner created", service.create(form, image, mobileImage), HttpStatus.CREATED.value()));
    }

    @PutMapping(value = "/api/v1/admin/home/banners/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestParam(required = false) String title,
                                    @RequestParam(required = false) String subtitle,
                                    @RequestParam(required = false) String buttonText,
                                    @RequestParam(required = false) String linkUrl,
                                    @RequestParam(required = false) String badgeText,
                                    @RequestParam(required = false) String keywords,
                                    @RequestParam(required = false) String bgColor,
                                    @RequestParam(required = false) String textColor,
                                    @RequestParam(required = false) String textAlign,
                                    @RequestParam(required = false) String startsAt,
                                    @RequestParam(required = false) String endsAt,
                                    @RequestParam(required = false) Boolean showCountdown,
                                    @RequestParam(required = false) Boolean active,
                                    @RequestParam(required = false) Boolean removeMobileImage,
                                    @RequestPart(required = false) MultipartFile image,
                                    @RequestPart(required = false) MultipartFile mobileImage) throws IOException {
        var form = new HomeContentService.Form(null, title, subtitle, buttonText, linkUrl, badgeText, bgColor, textColor,
                textAlign, startsAt, endsAt, showCountdown, active, removeMobileImage, keywords);
        return ResponseEntity.ok(GenericResponseDto.success("Banner updated", service.update(id, form, image, mobileImage), HttpStatus.OK.value()));
    }

    @PatchMapping("/api/v1/admin/home/banners/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> setActive(@PathVariable Long id, @RequestParam boolean value) {
        return ResponseEntity.ok(GenericResponseDto.success("Updated", service.setActive(id, value), HttpStatus.OK.value()));
    }

    @PostMapping("/api/v1/admin/home/banners/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> reorder(@RequestBody Map<String, List<Long>> body) {
        service.reorder(body.get("ids"));
        return ResponseEntity.ok(GenericResponseDto.success("Reordered", null, HttpStatus.OK.value()));
    }

    @DeleteMapping("/api/v1/admin/home/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(GenericResponseDto.success("Deleted", null, HttpStatus.OK.value()));
    }
}
