package ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;

public interface AdminExpenseController {

    ResponseEntity<?> create(String expenseJson, MultipartFile receipt, HttpServletRequest servletRequest) throws IOException;

    ResponseEntity<?> search(int pageNo, int pageSize, LocalDate start, LocalDate end,
                              String category, Boolean hasReceipt, String query);

    ResponseEntity<?> summary(LocalDate start, LocalDate end);

    ResponseEntity<?> monthlyTrend(String category, int months);
}
