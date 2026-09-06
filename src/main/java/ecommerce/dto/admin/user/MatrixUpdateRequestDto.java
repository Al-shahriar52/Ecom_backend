package ecommerce.dto.admin.user;

import lombok.Data;

@Data
public class MatrixUpdateRequestDto {
    private String role;
    private Integer permIndex;
    private Integer value;
}