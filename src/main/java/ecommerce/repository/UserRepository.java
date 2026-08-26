package ecommerce.repository;

import ecommerce.dto.UserDto;
import ecommerce.entity.AccountState;
import ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {

    @Query(value = "SELECT u FROM User u WHERE u.name LIKE CONCAT('%',:query,'%') " +
            "or u.phone like concat('%',:query,'%') " +
            "or u.email like concat('%',:query,'%') ")
    Page<User> search(Pageable pageable, String query);

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailOrPhoneAndStatusIsTrue(String email, String phone);
    Optional<User> findByEmailOrPhone(String email, String phone);

    @Query("SELECT u.id FROM User u WHERE u.email LIKE 'guest|_%@beautyhaat.internal' ESCAPE '|' " +
            "AND u.createdAt < :cutoff " +
            "AND u.id NOT IN (SELECT DISTINCT o.user.id FROM Order o)")
    List<Long> findStaleGuestUserIds(@Param("cutoff") String cutoff);

    long countByAccountState(AccountState accountState);

    long countByCreatedAtAfter(LocalDateTime date);
}
