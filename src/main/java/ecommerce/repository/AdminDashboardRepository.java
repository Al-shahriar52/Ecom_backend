package ecommerce.repository;

import ecommerce.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** Read-only numbers for the admin dashboard. */
public interface AdminDashboardRepository extends JpaRepository<Order, Long> {

    // ---- orders
    @Query("select o.createdAt, o.totalAmount, o.orderStatus, o.paymentMethod from Order o where o.createdAt >= :from")
    List<Object[]> orderRows(@Param("from") LocalDateTime from);

    @Query("select o.orderStatus, count(o) from Order o group by o.orderStatus")
    List<Object[]> statusCounts();

    @Query("select count(o) from Order o where o.orderStatus = ecommerce.enums.OrderStatus.PENDING and o.createdAt < :cutoff")
    long stalePending(@Param("cutoff") LocalDateTime cutoff);

    @Query("select o from Order o order by o.createdAt desc")
    List<Order> recent(Pageable pageable);

    @Query("select oi.product.id, oi.product.name, sum(oi.quantity), sum(oi.quantity * oi.price) from OrderItem oi " +
            "where oi.order.createdAt >= :from and oi.product is not null " +
            "and oi.order.orderStatus not in (ecommerce.enums.OrderStatus.CANCELLED, ecommerce.enums.OrderStatus.RETURNED) " +
            "group by oi.product.id, oi.product.name order by sum(oi.quantity) desc")
    List<Object[]> topProducts(@Param("from") LocalDateTime from, Pageable pageable);

    @Query("select pi.product.id, min(pi.imageUrl) from ProductImage pi where pi.product.id in :ids group by pi.product.id")
    List<Object[]> firstImages(@Param("ids") Collection<Long> ids);

    // ---- inventory
    @Query("select count(p) from Product p where p.status is null or p.status <> ecommerce.entity.ProductStatus.DRAFT")
    long publishedProducts();

    @Query("select count(p) from Product p where p.status = ecommerce.entity.ProductStatus.DRAFT")
    long draftProducts();

    @Query("select count(p) from Product p where (p.status is null or p.status <> ecommerce.entity.ProductStatus.DRAFT) and p.quantity <= 0")
    long outOfStockProducts();

    @Query("select count(p) from Product p where (p.status is null or p.status <> ecommerce.entity.ProductStatus.DRAFT) and p.quantity > 0 and p.quantity < 5")
    long lowStockProducts();

    @Query("select p.id, p.name, p.sku, p.quantity from Product p " +
            "where (p.status is null or p.status <> ecommerce.entity.ProductStatus.DRAFT) and p.quantity < 5 " +
            "order by p.quantity asc, p.name asc")
    List<Object[]> lowStockList(Pageable pageable);

    // ---- customers
    @Query("select count(u) from User u where u.email is null or u.email not like '%@beautyhaat.internal'")
    long customers();

    @Query("select count(u) from User u where u.createdAt >= :from and (u.email is null or u.email not like '%@beautyhaat.internal')")
    long newCustomers(@Param("from") LocalDateTime from);

    // ---- money out
    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.expenseDate between :a and :b")
    double expensesBetween(@Param("a") LocalDate a, @Param("b") LocalDate b);

    // ---- couriers
    @Query("select d.deliveryStatus, count(d) from Delivery d group by d.deliveryStatus")
    List<Object[]> deliveryCounts();
}
