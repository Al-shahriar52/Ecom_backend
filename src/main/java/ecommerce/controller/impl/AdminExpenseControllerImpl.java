package ecommerce.controller.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.controller.AdminExpenseController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.admin.CreateExpenseRequest;
import ecommerce.dto.admin.ExpenseDto;
import ecommerce.dto.admin.ExpenseSummaryDto;
import ecommerce.service.ExpenseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/expenses")
@RequiredArgsConstructor
public class AdminExpenseControllerImpl implements AdminExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> create(@RequestPart("expense") String expenseJson,
                                     @RequestPart(value = "receipt", required = false) MultipartFile receipt,
                                     HttpServletRequest servletRequest) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        CreateExpenseRequest request = mapper.readValue(expenseJson, CreateExpenseRequest.class);
        ExpenseDto dto = expenseService.create(request, receipt, servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Expense recorded successfully", dto, HttpStatus.CREATED.value()), HttpStatus.CREATED);
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> search(@RequestParam(defaultValue = "0") int pageNo,
                                     @RequestParam(defaultValue = "20") int pageSize,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) Boolean hasReceipt,
                                     @RequestParam(required = false) String query) {
        Page<ExpenseDto> page = expenseService.search(pageNo, pageSize, start, end, category, hasReceipt, query);
        return new ResponseEntity<>(GenericResponseDto.success("Expenses fetched successfully", page, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/summary")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> summary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        ExpenseSummaryDto dto = new ExpenseSummaryDto(
                expenseService.categoryTotals(start, end),
                expenseService.sumCogs(start, end),
                expenseService.sumOperating(start, end),
                expenseService.sumTotal(start, end)
        );
        return new ResponseEntity<>(GenericResponseDto.success("Expense summary fetched successfully", dto, HttpStatus.OK.value()), HttpStatus.OK);
    }

    @GetMapping("/monthly-trend")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<?> monthlyTrend(@RequestParam String category, @RequestParam(defaultValue = "6") int months) {
        Map<String, Double> data = expenseService.monthlyTrend(category, months);
        return new ResponseEntity<>(GenericResponseDto.success("Monthly trend fetched successfully", data, HttpStatus.OK.value()), HttpStatus.OK);
    }
}
