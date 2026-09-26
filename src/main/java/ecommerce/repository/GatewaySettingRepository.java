package ecommerce.repository;

import ecommerce.entity.GatewaySetting;
import ecommerce.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GatewaySettingRepository extends JpaRepository<GatewaySetting, Long> {
    Optional<GatewaySetting> findByPaymentMethod(PaymentMethod paymentMethod);
}