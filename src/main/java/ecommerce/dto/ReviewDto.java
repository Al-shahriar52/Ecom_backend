package ecommerce.dto;

import lombok.Data;

@Data
public class ReviewDto {

    private Long id;
    private String name;
    private String rating;
    private String comment;
    private String createdAt;
    private ProductDto product;
    private UserDto user;
}
