package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.UserDto;
import ecommerce.dto.admin.user.BulkImportResponseDto;
import ecommerce.dto.admin.user.UserDetailsResponseDto;
import ecommerce.dto.admin.user.UserRequestDto;
import ecommerce.dto.admin.user.UserStatsDto;
import ecommerce.service.AdminUserService;
import ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<GenericResponseDto<Map<String, Object>>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "all") String role,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "id") String sortKey,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        int pageNo = page > 0 ? page - 1 : 0; // Convert 1-indexed UI page to 0-indexed JPA page

        Map<String, Object> result = userService.getAdminUserList(pageNo, limit, search, role, status, sortKey, sortDir);

        return ResponseEntity.ok(
                GenericResponseDto.success("Users retrieved successfully", result, HttpStatus.OK.value())
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<GenericResponseDto<UserStatsDto>> getUserStats() {
        UserStatsDto stats = userService.getUserStats();
        return ResponseEntity.ok(
                GenericResponseDto.success("User statistics retrieved successfully", stats, HttpStatus.OK.value())
        );
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserRequestDto request) {
        UserDto createdUser = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto request) {
        UserDto updatedUser = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResponseDto> bulkImportUsers(@RequestParam("file") MultipartFile file) {
        BulkImportResponseDto result = adminUserService.importUsersFromCsv(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenericResponseDto<UserDetailsResponseDto>> getUserDetails(@PathVariable Long id) {
        // Fetch the aggregated user details from the service
        UserDetailsResponseDto userDetails = adminUserService.getUserDetails(id);

        return ResponseEntity.ok(
                GenericResponseDto.success("User details retrieved successfully", userDetails, HttpStatus.OK.value())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponseDto<String>> deleteUser(@PathVariable Long id) {
        adminUserService.anonymizeAndDeleteUser(id);

        return ResponseEntity.ok(
                GenericResponseDto.success("User deleted successfully", "User PII scrubbed and account deactivated", HttpStatus.OK.value())
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<GenericResponseDto<String>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean suspend) {

        adminUserService.toggleUserSuspension(id, suspend);

        String message = suspend ? "User account suspended" : "User account reactivated";
        return ResponseEntity.ok(
                GenericResponseDto.success(message, message, HttpStatus.OK.value())
        );
    }
}