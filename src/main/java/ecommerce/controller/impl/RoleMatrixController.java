package ecommerce.controller.impl;

import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.admin.user.MatrixUpdateRequestDto;
import ecommerce.dto.admin.user.RoleMatrixResponseDto;
import ecommerce.service.impl.RoleMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class RoleMatrixController {

    private final RoleMatrixService roleMatrixService;

    @GetMapping
    public ResponseEntity<RoleMatrixResponseDto> getRoleMatrix() {
        RoleMatrixResponseDto response = roleMatrixService.getRoleMatrix();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/matrix")
    public ResponseEntity<GenericResponseDto<String>> updateMatrix(@RequestBody MatrixUpdateRequestDto request) {
        roleMatrixService.updateMatrix(request);
        return ResponseEntity.ok(
                GenericResponseDto.success("Permissions updated successfully", null, HttpStatus.OK.value())
        );
    }
}