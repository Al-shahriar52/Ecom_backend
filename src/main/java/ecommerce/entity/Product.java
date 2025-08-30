package ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private ProductStatus status;

    @NotNull
    @Min(value = 1)
    private double originalPrice;

    @NotNull
    @Min(value = 0)
    private double discountedPrice;

    @NotNull
    @Min(value = 0)
    private int quantity;

    @NotNull
    @Column(unique = true)
    private String sku;

    private double rating;

    private Long numReviews;

    @ManyToOne
    private Brand brand;

    @ManyToOne
    private Tag tag;

    @ManyToOne
    private Category category;

    @ManyToOne
    private SubCategory subCategory;
}
