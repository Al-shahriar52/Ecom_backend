package ecommerce.dto;

import lombok.Data;

@Data
public class ProductDto {

    private Long id;
    private String name;
    private String image;
    private String description;
    private String brand;
    private String category;
    private double price;
    private int countInStock;
    private double rating;
    private Long numReviews;
}
