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
}
