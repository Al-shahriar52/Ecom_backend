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

    /** Permission indexes. The order is stored in the database, so only ever ADD to the end. */
    public static final int VIEW_DASHBOARD = 0, MANAGE_USERS = 1, CREATE_USERS = 2, EDIT_PERMISSIONS = 3, VIEW_ORDERS = 4,
            REFUND_ORDERS = 5, EXPORT_DATA = 6, MANAGE_SETTINGS = 7, MANAGE_COUPONS = 8, MANAGE_ACCOUNTING = 9, MANAGE_FBT = 10,
            MANAGE_PRODUCTS = 11, MANAGE_HOMEPAGE = 12, MANAGE_CATALOG = 13, CREATE_ORDERS = 14, MANAGE_COURIER = 15;

    private static final List<String> PERMISSIONS = Arrays.asList(
            "View dashboard",      // 0
            "Manage users",        // 1
            "Create users",        // 2
            "Edit permissions",    // 3
            "View orders",         // 4
            "Refund orders",       // 5
            "Export data",         // 6
            "Manage settings",     // 7
            "Manage Coupons",      // 8
            "Manage Accounting",   // 9
            "Manage FBT",          // 10
            "Manage products",     // 11
            "Manage homepage",     // 12
            "Manage catalog",      // 13 (brands, categories, cities, areas)
            "Create orders",       // 14 (admin-created orders)
            "Manage courier"       // 15
    );

    /** Defaults, also used to fill the new columns for roles saved before they existed. Admin always has everything. */
    private static final Map<String, List<Integer>> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("admin",   Collections.nCopies(PERMISSIONS.size(), 1));
        DEFAULTS.put("manager", Arrays.asList(1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 0));
        DEFAULTS.put("user",    Arrays.asList(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        DEFAULTS.put("guest",   Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        DEFAULTS.put("staff",   Arrays.asList(1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0));
    }

    private final RoleMatrixConfigRepository repository;
    private final ObjectMapper objectMapper;

    private RoleMatrixResponseDto getDefaultConfig() {
        RoleMatrixResponseDto dto = new RoleMatrixResponseDto();
        dto.setPermissions(new ArrayList<>(PERMISSIONS));
        dto.setRoleMeta(new HashMap<>());
        Map<String, List<Integer>> matrix = new HashMap<>();
        DEFAULTS.forEach((k, v) -> matrix.put(k, new ArrayList<>(v)));
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

    /** True when any of the given roles (e.g. "ROLE_STAFF") has this permission. Admin always does. */
    public boolean hasPermission(Collection<String> roles, int index) {
        if (roles == null) return false;
        Map<String, List<Integer>> matrix = null;
        for (String r : roles) {
            if (r == null) continue;
            String key = r.replaceFirst("^ROLE_", "").toLowerCase();
            if (key.equals("admin")) return true;
            if (matrix == null) matrix = getRoleMatrix().getMatrix();
            List<Integer> perms = matrix.get(key);
            if (perms != null && index >= 0 && index < perms.size() && Integer.valueOf(1).equals(perms.get(index))) return true;
        }
        return false;
    }

    @Transactional
    public void updateMatrix(MatrixUpdateRequestDto request) {
        RoleMatrixResponseDto currentConfig = getRoleMatrix();

        if (request.getRole() == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        String roleKey = request.getRole().toUpperCase().replace("ROLE_", "").toLowerCase();
        if (roleKey.equals("admin")) {
            throw new IllegalArgumentException("Administrator always has every permission.");
        }
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
        dto.setPermissions(new ArrayList<>(PERMISSIONS));
        int targetSize = PERMISSIONS.size();

        if (dto.getRoleMeta() == null) {
            dto.setRoleMeta(new HashMap<>());
        }
        Map<String, RoleMetaDto> meta = dto.getRoleMeta();
        meta.putIfAbsent("admin", new RoleMetaDto("Administrator", "purple"));
        meta.putIfAbsent("manager", new RoleMetaDto("Manager", "blue"));
        meta.putIfAbsent("user", new RoleMetaDto("User", "emerald"));
        meta.putIfAbsent("guest", new RoleMetaDto("Guest", "amber"));
        meta.putIfAbsent("staff", new RoleMetaDto("Staff", "slate"));

        Map<String, List<Integer>> normalizedMatrix = new HashMap<>();
        if (dto.getMatrix() != null) {
            for (Map.Entry<String, List<Integer>> entry : dto.getMatrix().entrySet()) {
                String cleanKey = entry.getKey().toUpperCase().replace("ROLE_", "").toLowerCase();
                List<Integer> perms = new ArrayList<>(entry.getValue());
                List<Integer> defaults = DEFAULTS.get(cleanKey);

                // roles saved before a permission existed get that permission's default for the role (or 0)
                while (perms.size() < targetSize) {
                    perms.add(defaults != null ? defaults.get(perms.size()) : 0);
                }
                if (perms.size() > targetSize) {
                    perms = new ArrayList<>(perms.subList(0, targetSize));
                }
                normalizedMatrix.put(cleanKey, perms);
            }
        }
        DEFAULTS.forEach((k, v) -> normalizedMatrix.putIfAbsent(k, new ArrayList<>(v)));
        normalizedMatrix.put("admin", new ArrayList<>(Collections.nCopies(targetSize, 1)));   // admin: always everything
        dto.setMatrix(normalizedMatrix);
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
