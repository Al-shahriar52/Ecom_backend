package ecommerce.repository;

import ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    @Query(value = "SELECT u FROM User u WHERE u.name LIKE CONCAT('%',:query,'%') " +
            "or u.phone like concat('%',:query,'%') " +
            "or u.email like concat('%',:query,'%') ")
    Page<User> search(Pageable pageable, String query);

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailOrPhoneAndStatusIsTrue(String email, String phone);

}
