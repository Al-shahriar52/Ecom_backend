package ecommerce.repository;

import ecommerce.entity.Invoice;
import ecommerce.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i.order.id FROM Invoice i WHERE i.invoiceNumber IN :invoices")
    List<Long> findOrderIdsByInvoiceNumbers(@Param("invoices") List<String> invoices);

    @Modifying
    @Query("UPDATE Invoice i SET i.status = :status WHERE i.invoiceNumber IN :invoices")
    int updateInvoiceStatuses(@Param("invoices") List<String> invoices, @Param("status") InvoiceStatus status);

    List<Invoice> findByOrder_IdIn(List<Long> orderIds);
}
