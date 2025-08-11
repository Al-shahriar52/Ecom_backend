package ecommerce.repository;

import ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = "SELECT o from Order o where o.user.phone like concat('%',:query,'%') ")
    Page<Order> search(Pageable pageable, String query);
}
