package ecommerce.dto.admin.user;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class RoleMatrixResponseDto {
    private Map<String, RoleMetaDto> roleMeta;
    private List<String> permissions;
    private Map<String, List<Integer>> matrix;
}