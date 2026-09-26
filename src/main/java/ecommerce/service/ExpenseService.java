package ecommerce.service;

import ecommerce.dto.admin.CreateExpenseRequest;
import ecommerce.dto.admin.ExpenseCategoryTotalDto;
import ecommerce.dto.admin.ExpenseDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExpenseService {

    ExpenseDto create(CreateExpenseRequest request, MultipartFile receipt, jakarta.servlet.http.HttpServletRequest servletRequest) throws IOException;

    Page<ExpenseDto> search(int pageNo, int pageSize, LocalDate start, LocalDate end,
                            String category, Boolean hasReceipt, String query);

    List<ExpenseCategoryTotalDto> categoryTotals(LocalDate start, LocalDate end);

    double sumCogs(LocalDate start, LocalDate end);
    double sumOperating(LocalDate start, LocalDate end);
    double sumTotal(LocalDate start, LocalDate end);

    /** Monthly totals for one category, oldest to newest - e.g. for the "last 6 months" chart. */
    Map<String, Double> monthlyTrend(String category, int months);

    List<ExpenseDto> recentRecurring();
}