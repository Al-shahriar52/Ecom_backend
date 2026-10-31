package ecommerce.service.impl;

import ecommerce.entity.HomeBanner;
import ecommerce.enums.BannerPlacement;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.HomeBannerRepository;
import ecommerce.utils.ImageUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/** Admin-managed home page content: hero slides, promo tiles and announcement lines. */
@Service
@RequiredArgsConstructor
public class HomeContentService {

    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Map<BannerPlacement, Integer> LIMITS = Map.of(
            BannerPlacement.HERO, 10, BannerPlacement.PROMO, 6, BannerPlacement.ANNOUNCEMENT, 5, BannerPlacement.CONCERN, 8);

    private final HomeBannerRepository repo;
    private final ImageUtil imageUtil;
    private final ecommerce.repository.ConcernProductRepository concernRepo;

    /** The edit form: everything optional except what each placement needs. */
    public record Form(BannerPlacement placement, String title, String subtitle, String buttonText, String linkUrl,
                       String badgeText, String bgColor, String textColor, String textAlign,
                       String startsAt, String endsAt, Boolean showCountdown, Boolean active,
                       Boolean removeMobileImage, String keywords) { }

    // ---------------------------------------------------------------- public

    /** What the storefront shows right now: active items inside their schedule. */
    public Map<String, Object> publicContent() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, List<Map<String, Object>>> out = new LinkedHashMap<>();
        out.put("hero", new ArrayList<>());
        out.put("promos", new ArrayList<>());
        out.put("announcements", new ArrayList<>());
        out.put("concerns", new ArrayList<>());
        for (HomeBanner b : repo.findAllByOrderByPlacementAscSortOrderAscIdAsc()) {
            if (!"LIVE".equals(statusOf(b, now))) continue;
            String key = switch (b.getPlacement()) {
                case HERO -> "hero";
                case PROMO -> "promos";
                case ANNOUNCEMENT -> "announcements";
                case CONCERN -> "concerns";
            };
            out.get(key).add(toMap(b, now, false));
        }
        return new LinkedHashMap<>(out);
    }

    // ---------------------------------------------------------------- admin

    public List<Map<String, Object>> list(BannerPlacement placement) {
        LocalDateTime now = LocalDateTime.now();
        List<HomeBanner> rows = placement == null ? repo.findAllByOrderByPlacementAscSortOrderAscIdAsc()
                : repo.findByPlacementOrderBySortOrderAscIdAsc(placement);
        List<Map<String, Object>> out = new ArrayList<>();
        for (HomeBanner b : rows) out.add(toMap(b, now, true));
        return out;
    }

    @Transactional
    public Map<String, Object> create(Form f, MultipartFile image, MultipartFile mobileImage) throws IOException {
        if (f.placement() == null) throw new BadRequestException("Choose where this banner is shown.");
        if (repo.countByPlacement(f.placement()) >= LIMITS.get(f.placement())) {
            throw new BadRequestException("You can have at most " + LIMITS.get(f.placement()) + " items here. Delete one first.");
        }
        HomeBanner b = new HomeBanner();
        b.setPlacement(f.placement());
        b.setSortOrder((int) repo.countByPlacement(f.placement()));
        apply(b, f, image, mobileImage, true);
        return toMap(repo.save(b), LocalDateTime.now(), true);
    }

    @Transactional
    public Map<String, Object> update(Long id, Form f, MultipartFile image, MultipartFile mobileImage) throws IOException {
        HomeBanner b = repo.findById(id).orElseThrow(() -> new BadRequestException("Banner not found."));
        apply(b, f, image, mobileImage, false);
        return toMap(repo.save(b), LocalDateTime.now(), true);
    }

    @Transactional
    public void delete(Long id) {
        HomeBanner b = repo.findById(id).orElseThrow(() -> new BadRequestException("Banner not found."));
        repo.delete(b);
    }

    @Transactional
    public Map<String, Object> setActive(Long id, boolean active) {
        HomeBanner b = repo.findById(id).orElseThrow(() -> new BadRequestException("Banner not found."));
        b.setActive(active);
        return toMap(repo.save(b), LocalDateTime.now(), true);
    }

    /** ids in the new display order (all from one placement). */
    @Transactional
    public void reorder(List<Long> ids) {
        if (ids == null) return;
        int i = 0;
        for (Long id : ids) {
            HomeBanner b = repo.findById(id).orElse(null);
            if (b != null) { b.setSortOrder(i++); repo.save(b); }
        }
    }

    // ---------------------------------------------------------------- helpers

    private void apply(HomeBanner b, Form f, MultipartFile image, MultipartFile mobileImage, boolean creating) throws IOException {
        BannerPlacement p = b.getPlacement();
        String title = clean(f.title(), 160);
        String link = clean(f.linkUrl(), 1000);
        if (link != null && !(link.startsWith("/") || link.startsWith("https://") || link.startsWith("http://"))) {
            throw new BadRequestException("Link must start with / (a page on your site) or https://");
        }
        if (p == BannerPlacement.ANNOUNCEMENT) {
            if (title == null) throw new BadRequestException("Write the announcement text.");
        } else if (p == BannerPlacement.CONCERN) {
            if (title == null) throw new BadRequestException("Write the concern name, e.g. Acne.");
            if (cleanKeywords(f.keywords()).isEmpty()) throw new BadRequestException("Add at least one keyword, e.g. acne, pimple.");
            link = null;
        } else {
            boolean hasImage = (image != null && !image.isEmpty()) || (b.getImageUrl() != null && !b.getImageUrl().isBlank());
            if (!hasImage) throw new BadRequestException("Upload a banner image.");
        }

        if (image != null && !image.isEmpty()) {
            checkImage(image);
            b.setImageUrl(imageUtil.uploadFile(image));
        }
        if (mobileImage != null && !mobileImage.isEmpty()) {
            checkImage(mobileImage);
            b.setMobileImageUrl(imageUtil.uploadFile(mobileImage));
        } else if (Boolean.TRUE.equals(f.removeMobileImage())) {
            b.setMobileImageUrl(null);
        }

        b.setTitle(title);
        b.setSubtitle(clean(f.subtitle(), 500));
        b.setButtonText(clean(f.buttonText(), 60));
        b.setLinkUrl(link);
        b.setBadgeText(clean(f.badgeText(), 30));
        b.setKeywords(p == BannerPlacement.CONCERN ? String.join(", ", cleanKeywords(f.keywords())) : null);
        b.setBgColor(color(f.bgColor()));
        b.setTextColor(color(f.textColor()));
        String align = f.textAlign() == null ? "left" : f.textAlign().toLowerCase();
        b.setTextAlign(List.of("left", "center", "right").contains(align) ? align : "left");

        LocalDateTime s = parse(f.startsAt());
        LocalDateTime e = parse(f.endsAt());
        if (s != null && e != null && !e.isAfter(s)) throw new BadRequestException("The end time must be after the start time.");
        b.setStartsAt(s);
        b.setEndsAt(e);
        b.setShowCountdown(Boolean.TRUE.equals(f.showCountdown()) && e != null);
        if (f.active() != null) b.setActive(f.active());
        else if (creating) b.setActive(true);
    }

    private static void checkImage(MultipartFile f) {
        String ct = f.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("image/")) throw new BadRequestException("Only image files can be uploaded.");
        if (f.getSize() > 8L * 1024 * 1024) throw new BadRequestException("Image is too large (8 MB max).");
    }

    /** "Acne, pimple ,blemish" -> [acne, pimple, blemish] (max 5, lower case). */
    public static List<String> cleanKeywords(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String k : raw.split("[,\\n]")) {
            String t = k.trim().toLowerCase().replace("%", "").replace("_", " ");
            if (t.length() >= 2 && t.length() <= 40 && !out.contains(t)) out.add(t);
            if (out.size() == 5) break;
        }
        return out;
    }

    /** Products whose name, description, sub-category or tag mention any keyword. */
    public Map<String, Object> concernProducts(String keywords, int page, int size) {
        List<String> k = cleanKeywords(keywords);
        if (k.isEmpty()) throw new BadRequestException("No keywords given.");
        String[] p = new String[5];
        for (int i = 0; i < 5; i++) p[i] = i < k.size() ? "%" + k.get(i) + "%" : "\u0001no-match\u0001";
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 48),
                org.springframework.data.domain.Sort.by("rating").descending());
        var res = concernRepo.findByKeywords(pageable, p[0], p[1], p[2], p[3], p[4]);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("products", res.getContent());
        m.put("total", res.getTotalElements());
        m.put("hasMore", res.hasNext());
        return m;
    }

    private static String clean(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static String color(String c) {
        return c != null && COLOR.matcher(c.trim()).matches() ? c.trim() : null;
    }

    private static LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String t = s.trim();
            if (t.length() == 16) t += ":00";               // datetime-local gives yyyy-MM-ddTHH:mm
            return LocalDateTime.parse(t);
        } catch (Exception e) {
            throw new BadRequestException("Date and time is not valid.");
        }
    }

    private static String statusOf(HomeBanner b, LocalDateTime now) {
        if (!b.isActive()) return "OFF";
        if (b.getStartsAt() != null && now.isBefore(b.getStartsAt())) return "SCHEDULED";
        if (b.getEndsAt() != null && !now.isBefore(b.getEndsAt())) return "EXPIRED";
        return "LIVE";
    }

    private static Map<String, Object> toMap(HomeBanner b, LocalDateTime now, boolean admin) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("placement", b.getPlacement().name());
        m.put("title", b.getTitle());
        m.put("subtitle", b.getSubtitle());
        m.put("buttonText", b.getButtonText());
        m.put("linkUrl", b.getLinkUrl());
        m.put("imageUrl", b.getImageUrl());
        m.put("mobileImageUrl", b.getMobileImageUrl());
        m.put("badgeText", b.getBadgeText());
        m.put("keywords", b.getKeywords());
        m.put("bgColor", b.getBgColor());
        m.put("textColor", b.getTextColor());
        m.put("textAlign", b.getTextAlign());
        m.put("showCountdown", b.isShowCountdown());
        m.put("endsAt", b.getEndsAt() != null ? b.getEndsAt().toString() : null);
        if (admin) {
            m.put("active", b.isActive());
            m.put("startsAt", b.getStartsAt() != null ? b.getStartsAt().toString() : null);
            m.put("sortOrder", b.getSortOrder());
            m.put("status", statusOf(b, now));
        }
        return m;
    }
}
