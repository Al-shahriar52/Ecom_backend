package ecommerce.repository;

import ecommerce.entity.User;
import ecommerce.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, Long> {

    List<WishList> findByUser(User user);
    Optional<WishList> findByUserAndProductId(User user, Long productId);
    void deleteByUserAndProductId(User user, Long productId);

    @Query("SELECT DISTINCT w.product.category.id FROM WishList w " +
            "WHERE w.user.id = :userId AND w.product.category IS NOT NULL")
    List<Long> findDistinctCategoryIdsByUserId(Long userId);

    @Query("SELECT w.product.id FROM WishList w WHERE w.user.id = :userId")
    List<Long> findProductIdsByUserId(Long userId);
}