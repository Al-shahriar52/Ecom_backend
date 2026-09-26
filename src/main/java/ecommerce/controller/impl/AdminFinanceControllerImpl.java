package ecommerce.controller.impl;

import ecommerce.controller.AdminFinanceController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.admin.FinanceDashboardDto;
import ecommerce.dto.admin.UpdateGatewayRateRequest;
import ecommerce.service.FinanceDashboardService;
import ecommerce.service.FinanceCodService;
import ecommerce.service.FinancePLService;
import ecommerce.service.FinanceReportService;
import ecommerce.service.GatewaySettingService;
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
    private final FinanceCodService financeCodService;
    private final FinancePLService financePLService;
    private final FinanceReportService financeReportService;
    private final GatewaySettingService gatewaySettingService;

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

    @GetMapping("/cod")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> getCodSummary() {
        return new ResponseEntity<>(GenericResponseDto.success("COD summary fetched successfully", financeCodService.getCodSummary(), HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/profit-loss")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> getProfitLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return new ResponseEntity<>(GenericResponseDto.success("Profit and loss fetched successfully", financePLService.getProfitLoss(start, end), HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/report")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> getReport(
            @RequestParam(required = false, defaultValue = "week") String grain,
            @RequestParam(required = false, defaultValue = "12") int periods) {
        return new ResponseEntity<>(GenericResponseDto.success("Report fetched successfully", financeReportService.getWeeklyOrMonthlyReport(grain, periods), HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/gateways")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> getGateways() {
        return new ResponseEntity<>(GenericResponseDto.success("Gateways fetched successfully", gatewaySettingService.getAllWithStats(), HttpStatus.OK.value()), HttpStatus.OK);
    }

    @PatchMapping("/gateways/{paymentMethod}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> updateGatewayRate(@PathVariable String paymentMethod, @RequestBody UpdateGatewayRateRequest request) {
        return new ResponseEntity<>(GenericResponseDto.success("Gateway updated successfully", gatewaySettingService.updateRate(paymentMethod, request.getFeeRatePct()), HttpStatus.OK.value()), HttpStatus.OK);
    }
}