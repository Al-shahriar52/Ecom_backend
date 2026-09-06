package ecommerce.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.dto.admin.user.MatrixUpdateRequestDto;
import ecommerce.dto.admin.user.RoleMatrixResponseDto;
import ecommerce.dto.admin.user.RoleMetaDto;
import ecommerce.entity.RoleMatrixConfig;
import ecommerce.repository.RoleMatrixConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoleMatrixService {

    private final RoleMatrixConfigRepository repository;
    private final ObjectMapper objectMapper;

    private RoleMatrixResponseDto getDefaultConfig() {
        RoleMatrixResponseDto dto = new RoleMatrixResponseDto();

        List<String> permissions = Arrays.asList(
                "View dashboard",     // Index 0
                "Manage users",       // Index 1
                "Create users",       // Index 2
                "Edit permissions",   // Index 3
                "View orders",        // Index 4
                "Refund orders",      // Index 5
                "Export data",        // Index 6
                "Manage settings",    // Index 7
                "Manage Coupons",     // Index 8
                "Manage Accounting",  // Index 9
                "Manage FBT"          // Index 10
        );
        dto.setPermissions(permissions);

        // Seed metadata for ALL fixed roles from the Role enum
        Map<String, RoleMetaDto> meta = new HashMap<>();
        meta.put("admin", new RoleMetaDto("Administrator", "purple"));
        meta.put("manager", new RoleMetaDto("Manager", "blue"));
        meta.put("user", new RoleMetaDto("User", "emerald"));
        meta.put("guest", new RoleMetaDto("Guest", "amber"));
        meta.put("staff", new RoleMetaDto("Staff", "slate"));
        dto.setRoleMeta(meta);

        // Seed default permission arrays (11 items each) for ALL roles
        Map<String, List<Integer>> matrix = new HashMap<>();
        matrix.put("admin", Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1));
        matrix.put("manager", Arrays.asList(1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0));
        matrix.put("user", Arrays.asList(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0));
        matrix.put("guest", Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        matrix.put("staff", Arrays.asList(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0));
        dto.setMatrix(matrix);

        return dto;
    }

    @Transactional
    public RoleMatrixResponseDto getRoleMatrix() {
        List<RoleMatrixConfig> configs = repository.findAll();

        if (configs.isEmpty()) {
            RoleMatrixResponseDto defaultDto = getDefaultConfig();
            saveConfig(defaultDto);
            return defaultDto;
        }

        try {
            RoleMatrixResponseDto configDto = objectMapper.readValue(configs.get(0).getConfigData(), RoleMatrixResponseDto.class);
            normalizeAndSyncConfig(configDto);
            return configDto;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing role matrix config", e);
        }
    }

    @Transactional
    public void updateMatrix(MatrixUpdateRequestDto request) {
        RoleMatrixResponseDto currentConfig = getRoleMatrix();

        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        String roleKey = request.getRole().toUpperCase().replace("ROLE_", "").toLowerCase();
        Map<String, List<Integer>> matrix = currentConfig.getMatrix();

        if (matrix.containsKey(roleKey)) {
            List<Integer> perms = matrix.get(roleKey);

            if (request.getPermIndex() >= 0 && request.getPermIndex() < perms.size()) {
                perms.set(request.getPermIndex(), request.getValue());
                saveConfig(currentConfig);
            } else {
                throw new IllegalArgumentException("Invalid permission index: " + request.getPermIndex());
            }
        } else {
            throw new IllegalArgumentException("Role not found in matrix: " + roleKey);
        }
    }

    private void normalizeAndSyncConfig(RoleMatrixResponseDto dto) {
        List<String> currentPermissions = Arrays.asList(
                "View dashboard", "Manage users", "Create users", "Edit permissions",
                "View orders", "Refund orders", "Export data", "Manage settings",
                "Manage Coupons", "Manage Accounting", "Manage FBT"
        );
        dto.setPermissions(currentPermissions);
        int targetSize = currentPermissions.size();

        // Ensure default meta items exist for standard roles if missing
        if (dto.getRoleMeta() == null) {
            dto.setRoleMeta(new HashMap<>());
        }
        Map<String, RoleMetaDto> meta = dto.getRoleMeta();
        meta.putIfAbsent("admin", new RoleMetaDto("Administrator", "purple"));
        meta.putIfAbsent("manager", new RoleMetaDto("Manager", "blue"));
        meta.putIfAbsent("user", new RoleMetaDto("User", "emerald"));
        meta.putIfAbsent("guest", new RoleMetaDto("Guest", "amber"));
        meta.putIfAbsent("staff", new RoleMetaDto("Staff", "slate"));

        if (dto.getMatrix() != null) {
            Map<String, List<Integer>> normalizedMatrix = new HashMap<>();
            for (Map.Entry<String, List<Integer>> entry : dto.getMatrix().entrySet()) {
                String cleanKey = entry.getKey().toUpperCase().replace("ROLE_", "").toLowerCase();
                List<Integer> perms = new ArrayList<>(entry.getValue());

                while (perms.size() < targetSize) {
                    perms.add(0);
                }
                if (perms.size() > targetSize) {
                    perms = perms.subList(0, targetSize);
                }

                normalizedMatrix.put(cleanKey, perms);
            }

            // Ensure all base roles have an entry in the matrix
            normalizedMatrix.putIfAbsent("admin", new ArrayList<>(Collections.nCopies(targetSize, 1)));
            normalizedMatrix.putIfAbsent("manager", new ArrayList<>(Arrays.asList(1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0)));
            normalizedMatrix.putIfAbsent("user", new ArrayList<>(Arrays.asList(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0)));
            normalizedMatrix.putIfAbsent("guest", new ArrayList<>(Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
            normalizedMatrix.putIfAbsent("staff", new ArrayList<>(Arrays.asList(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0)));

            dto.setMatrix(normalizedMatrix);
        }
    }

    private void saveConfig(RoleMatrixResponseDto dto) {
        try {
            normalizeAndSyncConfig(dto);
            String json = objectMapper.writeValueAsString(dto);
            RoleMatrixConfig config;

            List<RoleMatrixConfig> configs = repository.findAll();
            config = configs.isEmpty() ? new RoleMatrixConfig() : configs.get(0);

            config.setConfigData(json);
            repository.save(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error saving role matrix config", e);
        }
    }
}