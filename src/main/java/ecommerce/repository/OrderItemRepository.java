package ecommerce.repository;

import ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT DISTINCT oi.product.category.id FROM OrderItem oi " +
            "WHERE oi.order.user.id = :userId AND oi.product.category IS NOT NULL")
    List<Long> findDistinctCategoryIdsByUserId(Long userId);
}