package ecommerce.repository;

import ecommerce.entity.Order;
import ecommerce.entity.User;
import ecommerce.enums.DeliveryStatus;
import ecommerce.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query(value = "SELECT o from Order o where o.user.phone like concat('%',:query,'%') ")
    Page<Order> search(Pageable pageable, String query);

    Page<Order> findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Total Orders
    long count();

    // Cancelled Orders
    long countByOrderStatus(OrderStatus orderStatus);

    // Delivery Status Counts (Joins with the Delivery table)
    @Query("SELECT COUNT(o) FROM Order o JOIN o.delivery d WHERE d.deliveryStatus = :status")
    long countByDeliveryStatus(@Param("status") DeliveryStatus status);

    // Pending Pickup (Orders that are CONFIRMED but have no delivery record yet, or delivery is PENDING)
    @Query("SELECT COUNT(o) FROM Order o LEFT JOIN o.delivery d WHERE (o.orderStatus = 'CONFIRMED' AND d IS NULL) OR (d.deliveryStatus = 'PENDING')")
    long countPendingPickup();

    // Total Balance (Sum of totalAmount for all PAID orders)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PAID'")
    Double sumTotalBalance();

    @Modifying
    @Query("UPDATE Order o SET o.orderStatus = :status WHERE o.id IN :orderIds")
    int updateOrderStatusesByIds(@Param("orderIds") List<Long> orderIds, @Param("status") ecommerce.enums.OrderStatus status);

    List<Order> findByUserId(Long userId);

    long countByUser(User user);

    List<Order> findByCouponCode(String couponCode);

    // ============ FINANCE DASHBOARD ============

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end AND o.orderStatus <> 'CANCELLED'")
    double sumGrossSales(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end AND o.paymentStatus = 'REFUNDED'")
    double sumRefunds(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end AND o.paymentStatus = 'PAID'")
    long countPaidOrders(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT o.paymentMethod, COALESCE(SUM(o.totalAmount), 0), COUNT(o) FROM Order o " +
            "WHERE o.createdAt >= :start AND o.createdAt < :end AND o.orderStatus <> 'CANCELLED' GROUP BY o.paymentMethod")
    List<Object[]> gatewayMix(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paidAt >= :start AND o.paidAt < :end AND o.paymentStatus = 'PAID'")
    double sumCollected(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PENDING' AND o.paymentMethod <> 'COD' AND o.orderStatus <> 'CANCELLED'")
    double sumAwaitingSettlement();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = 'PENDING' AND o.paymentMethod <> 'COD' AND o.orderStatus <> 'CANCELLED'")
    long countAwaitingSettlement();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o JOIN o.delivery d " +
            "WHERE o.paymentMethod = 'COD' AND d.deliveryStatus IN ('READY_FOR_PICKUP', 'IN_TRANSIT')")
    double sumCodPending();

    @Query("SELECT COUNT(o) FROM Order o JOIN o.delivery d " +
            "WHERE o.paymentMethod = 'COD' AND d.deliveryStatus IN ('READY_FOR_PICKUP', 'IN_TRANSIT')")
    long countCodPending();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o JOIN o.delivery d " +
            "WHERE o.paymentMethod = 'COD' AND d.deliveryStatus = 'DELIVERED' AND o.paymentStatus <> 'PAID'")
    double sumCodDeliveredUnpaid();

    @Query("SELECT COUNT(o) FROM Order o JOIN o.delivery d " +
            "WHERE o.paymentMethod = 'COD' AND d.deliveryStatus = 'DELIVERED' AND o.paymentStatus <> 'PAID'")
    long countCodDeliveredUnpaid();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o JOIN o.delivery d " +
            "WHERE o.paymentMethod = 'COD' AND d.deliveryStatus = 'RETURNED'")
    double sumCodReturned();

    @Query("SELECT COUNT(o) FROM Order o JOIN o.delivery d " +
            "WHERE o.paymentMethod = 'COD' AND d.deliveryStatus = 'RETURNED'")
    long countCodReturned();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentMethod = 'COD' AND o.delivery IS NOT NULL")
    long countAllCodWithDelivery();

    @Query("SELECT COALESCE(d.courierName, 'Unassigned'), COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
            "FROM Order o JOIN o.delivery d WHERE o.paymentMethod = 'COD' GROUP BY d.courierName")
    List<Object[]> codCourierMix();

    @Query("SELECT o FROM Order o JOIN o.delivery d WHERE o.paymentMethod = 'COD' ORDER BY o.createdAt DESC")
    List<Order> findCodOrders(org.springframework.data.domain.Pageable pageable);
}