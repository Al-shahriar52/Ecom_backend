package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<GenericResponseDto<Map<String, Object>>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "all") String role,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "id") String sortKey,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        int pageNo = page > 0 ? page - 1 : 0; // Convert 1-indexed UI page to 0-indexed JPA page

        Map<String, Object> result = userService.getAdminUserList(pageNo, limit, search, role, status, sortKey, sortDir);

        return ResponseEntity.ok(
                GenericResponseDto.success("Users retrieved successfully", result, HttpStatus.OK.value())
        );
    }
}