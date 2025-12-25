package ecommerce.repository;

import ecommerce.entity.User;
import ecommerce.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, Long> {

    List<WishList> findByUser(User user);
    Optional<WishList> findByUserAndProductId(User user, Long productId);
    void deleteByUserAndProductId(User user, Long productId);
}
