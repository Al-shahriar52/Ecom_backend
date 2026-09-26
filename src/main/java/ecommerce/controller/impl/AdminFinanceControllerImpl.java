package ecommerce.controller.impl;

import ecommerce.controller.AdminFinanceController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.admin.FinanceDashboardDto;
import ecommerce.service.FinanceDashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceControllerImpl implements AdminFinanceController {

    private final FinanceDashboardService financeDashboardService;

    @GetMapping("/dashboard")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false, defaultValue = "week") String grain) {
        FinanceDashboardDto dto = financeDashboardService.getDashboard(start, end, grain);
        return new ResponseEntity<>(GenericResponseDto.success("Dashboard fetched successfully", dto, HttpStatus.OK.value()), HttpStatus.OK);
    }
}
