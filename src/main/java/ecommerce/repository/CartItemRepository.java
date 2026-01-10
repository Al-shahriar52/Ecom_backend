package ecommerce.repository;

import ecommerce.entity.Cart;
import ecommerce.entity.CartItem;
import ecommerce.entity.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    List<CartItem> findByCartAndIsActiveTrue(Cart cart);
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.cart.id = :cartId")
    void deleteAllByCartId(Long cartId);
}
