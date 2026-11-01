package ecommerce.repository;

import ecommerce.entity.Delivery;
import ecommerce.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Read-only numbers for a customer's own account dashboard. */
public interface CustomerDashboardRepository extends JpaRepository<Order, Long> {

    @Query("select o.orderStatus, count(o), coalesce(sum(o.totalAmount), 0) from Order o where o.user.id = :uid group by o.orderStatus")
    List<Object[]> statusTotals(@Param("uid") Long uid);

    @Query("select o from Order o where o.user.id = :uid order by o.createdAt desc")
    List<Order> latest(@Param("uid") Long uid, Pageable pageable);

    @Query("select o from Order o where o.user.id = :uid and o.orderStatus in " +
            "(ecommerce.enums.OrderStatus.PENDING, ecommerce.enums.OrderStatus.CONFIRMED, ecommerce.enums.OrderStatus.PACKAGED, ecommerce.enums.OrderStatus.SHIPPED) " +
            "order by o.createdAt desc")
    List<Order> openOrders(@Param("uid") Long uid, Pageable pageable);

    @Query("select oi.order.id, oi.product.id, oi.product.name, oi.quantity, oi.product.slug from OrderItem oi " +
            "where oi.order.id in :orderIds and oi.product is not null order by oi.id asc")
    List<Object[]> itemsOf(@Param("orderIds") Collection<Long> orderIds);

    @Query("select pi.product.id, min(pi.imageUrl) from ProductImage pi where pi.product.id in :ids group by pi.product.id")
    List<Object[]> firstImages(@Param("ids") Collection<Long> ids);

    @Query("select d from Delivery d where d.order.id = :orderId")
    List<Delivery> deliveryOf(@Param("orderId") Long orderId);

    @Query("select count(a) from Address a where a.userId = :uid")
    long addressCount(@Param("uid") Long uid);

    @Query("select count(w) from WishList w where w.user.id = :uid")
    long wishlistCount(@Param("uid") Long uid);
}
