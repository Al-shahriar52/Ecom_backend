package ecommerce.service.impl;

import ecommerce.dto.admin.CreateExpenseRequest;
import ecommerce.dto.admin.ExpenseCategoryTotalDto;
import ecommerce.dto.admin.ExpenseDto;
import ecommerce.entity.Expense;
import ecommerce.entity.User;
import ecommerce.enums.ExpenseCategory;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.ExpenseRepository;
import ecommerce.service.ExpenseService;
import ecommerce.utils.ImageUtil;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ImageUtil imageUtil;
    private final TokenUtil tokenUtil;

    @Override
    public ExpenseDto create(CreateExpenseRequest request, MultipartFile receipt, HttpServletRequest servletRequest) throws IOException {
        if (request.getAmount() <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new BadRequestException("Category is required");
        }

        Expense expense = new Expense();
        try {
            expense.setCategory(ExpenseCategory.valueOf(request.getCategory()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid category: " + request.getCategory());
        }
        expense.setVendor(request.getVendor());
        expense.setReference(request.getReference());
        expense.setPaidFrom(request.getPaidFrom());
        expense.setAmount(request.getAmount());
        expense.setVatAmount(request.getVatAmount());
        expense.setAitAmount(request.getAitAmount());
        expense.setNote(request.getNote());
        expense.setRecurring(request.isRecurring());
        expense.setCogs(request.isCogs());
        expense.setExpenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now());

        try {
            User user = tokenUtil.extractUserInfo(servletRequest);
            expense.setCreatedBy(user);
        } catch (Exception ignored) {
            // Shouldn't happen behind an admin-only route, but don't hard-fail on it.
        }

        if (receipt != null && !receipt.isEmpty()) {
            expense.setReceiptUrl(imageUtil.uploadRawFile(receipt));
        }

        Expense saved = expenseRepository.save(expense);
        return toDto(saved);
    }

    @Override
    public Page<ExpenseDto> search(int pageNo, int pageSize, LocalDate start, LocalDate end,
                                    String category, Boolean hasReceipt, String query) {
        ExpenseCategory categoryEnum = null;
        if (category != null && !category.isBlank()) {
            try {
                categoryEnum = ExpenseCategory.valueOf(category);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid category: " + category);
            }
        }
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();
        return expenseRepository.search(pageable, start, end, categoryEnum, hasReceipt, normalizedQuery)
                .map(this::toDto);
    }

    @Override
    public List<ExpenseCategoryTotalDto> categoryTotals(LocalDate start, LocalDate end) {
        return expenseRepository.categoryTotals(start, end);
    }

    @Override
    public double sumCogs(LocalDate start, LocalDate end) {
        return expenseRepository.sumCogs(start, end);
    }

    @Override
    public double sumOperating(LocalDate start, LocalDate end) {
        return expenseRepository.sumOperating(start, end);
    }

    @Override
    public double sumTotal(LocalDate start, LocalDate end) {
        return expenseRepository.sumTotal(start, end);
    }

    @Override
    public Map<String, Double> monthlyTrend(String category, int months) {
        ExpenseCategory categoryEnum;
        try {
            categoryEnum = ExpenseCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid category: " + category);
        }

        Map<String, Double> result = new LinkedHashMap<>();
        YearMonth current = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            double total = expenseRepository.sumForCategoryAndRange(categoryEnum, start, end);
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            result.put(label, total);
        }
        return result;
    }

    private ExpenseDto toDto(Expense e) {
        ExpenseDto dto = new ExpenseDto();
        dto.setId(e.getId());
        dto.setCategory(e.getCategory() != null ? e.getCategory().name() : null);
        dto.setVendor(e.getVendor());
        dto.setReference(e.getReference());
        dto.setPaidFrom(e.getPaidFrom());
        dto.setAmount(e.getAmount());
        dto.setVatAmount(e.getVatAmount());
        dto.setAitAmount(e.getAitAmount());
        dto.setNote(e.getNote());
        dto.setRecurring(e.isRecurring());
        dto.setCogs(e.isCogs());
        dto.setReceiptUrl(e.getReceiptUrl());
        dto.setExpenseDate(e.getExpenseDate());
        return dto;
    }
}
