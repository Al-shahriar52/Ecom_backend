package ecommerce.entity;

import ecommerce.enums.BannerPlacement;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** One admin-managed item on the home page: a hero slide, a promo tile or an announcement line. */
@Entity
@Data
@Table(name = "home_banners")
public class HomeBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BannerPlacement placement;

    private String title;                 // headline (or the announcement text)
    @Column(length = 500)
    private String subtitle;
    private String buttonText;
    @Column(length = 1000)
    private String linkUrl;               // "/category/skin" or a full https:// address

    @Column(length = 1000)
    private String imageUrl;              // wide / desktop image
    @Column(length = 1000)
    private String mobileImageUrl;        // optional portrait / square image for phones

    @Column(length = 500)
    private String keywords;              // CONCERN only: "acne, pimple, blemish" - used to find matching products

    private String badgeText;             // small pill, e.g. "NEW" or "-30%"
    private String bgColor;               // #rrggbb (announcement bar / text-only tiles)
    private String textColor;             // #rrggbb
    private String textAlign;             // left | center | right

    private int sortOrder = 0;
    private boolean active = true;
    private LocalDateTime startsAt;       // null = no start limit
    private LocalDateTime endsAt;         // null = no end limit
    private boolean showCountdown = false;

    private LocalDateTime createdAt = LocalDateTime.now();
}
