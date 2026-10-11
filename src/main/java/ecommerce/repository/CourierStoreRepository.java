package ecommerce.repository;

import ecommerce.entity.CourierStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourierStoreRepository extends JpaRepository<CourierStore, Long> {
    List<CourierStore> findByProviderOrderByDefaultStoreDescNameAsc(String provider);
    List<CourierStore> findByProviderAndActiveTrueOrderByDefaultStoreDescIdAsc(String provider);
    Optional<CourierStore> findByProviderAndExternalStoreId(String provider, Long externalStoreId);
}
