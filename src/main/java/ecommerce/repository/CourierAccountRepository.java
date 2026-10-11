package ecommerce.repository;

import ecommerce.entity.CourierAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourierAccountRepository extends JpaRepository<CourierAccount, Long> {
    Optional<CourierAccount> findByProvider(String provider);
    Optional<CourierAccount> findFirstByEnabledTrueAndDefaultProviderTrue();
}
