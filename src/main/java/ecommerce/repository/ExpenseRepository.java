package ecommerce.repository;

import ecommerce.entity.Expense;
import ecommerce.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE " +
            "(:start IS NULL OR e.expenseDate >= :start) AND (:end IS NULL OR e.expenseDate <= :end) AND " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:hasReceipt IS NULL OR (:hasReceipt = TRUE AND e.receiptUrl IS NOT NULL) OR (:hasReceipt = FALSE AND e.receiptUrl IS NULL)) AND " +
            "(:query IS NULL OR LOWER(e.vendor) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.reference) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY e.expenseDate DESC, e.id DESC")
    Page<Expense> search(Pageable pageable,
                          @Param("start") LocalDate start,
                          @Param("end") LocalDate end,
                          @Param("category") ExpenseCategory category,
                          @Param("hasReceipt") Boolean hasReceipt,
                          @Param("query") String query);

    @Query("SELECT new ecommerce.dto.admin.ExpenseCategoryTotalDto(CAST(e.category AS string), SUM(e.amount), COUNT(e)) " +
            "FROM Expense e WHERE e.expenseDate >= :start AND e.expenseDate <= :end " +
            "GROUP BY e.category")
    List<ecommerce.dto.admin.ExpenseCategoryTotalDto> categoryTotals(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate >= :start AND e.expenseDate <= :end AND e.cogs = TRUE")
    double sumCogs(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate >= :start AND e.expenseDate <= :end AND e.cogs = FALSE")
    double sumOperating(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate >= :start AND e.expenseDate <= :end")
    double sumTotal(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.category = :category AND e.expenseDate >= :start AND e.expenseDate <= :end")
    double sumForCategoryAndRange(@Param("category") ExpenseCategory category, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
