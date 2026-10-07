package ecommerce.entity;

import ecommerce.enums.SocialProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Links a User to a Google/Facebook identity.
 * Kept in its own table so the existing User entity is not changed at all.
 */
@Entity
@Table(
        name = "social_account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_provider_provider_id",
                columnNames = {"provider", "provider_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ON DELETE CASCADE so deleting a user (e.g. from the admin panel) still works.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}