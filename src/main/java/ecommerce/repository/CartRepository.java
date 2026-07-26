package ecommerce.repository;

import ecommerce.entity.Cart;
import ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    @Modifying
    @Query("DELETE FROM Cart c WHERE c.user.id IN :userIds")
    void deleteByUserIdIn(@Param("userIds") List<Long> userIds);

}
