package ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Lob
    private String image;

    private String description;

    @NotBlank
    private String brand;

    @NotBlank
    private String category;

    @NotNull
    @Min(value = 1)
    private double price;

    @NotNull
    @Min(value = 0)
    private int countInStock;

    private double rating;

    private Long numReviews;
}
