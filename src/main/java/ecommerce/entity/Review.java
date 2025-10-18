package ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    @Min(value = 1)
    @Max(value = 5)
    private Double rating;

    @NotNull
    private String comment;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    private Product product;

    @ManyToOne
    private User user;

    private String imageUrl;

    private  boolean isApproved;
}
