package ecommerce.repository;

import ecommerce.dto.courier.AwaitingPickupDto;
import ecommerce.dto.courier.SyncTargetDto;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Optional;
import ecommerce.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findFirstByCourierNameAndConsignmentId(String courierName, String consignmentId);

    /** Parcels already booked with Steadfast that no rider has collected yet. */
    @Query("SELECT new ecommerce.dto.courier.AwaitingPickupDto(" +
            "o.id, i.invoiceNumber, d.consignmentId, o.name, o.phoneNumber, o.city, d.codAmount, d.requestedAt) " +
            "FROM Delivery d JOIN d.order o LEFT JOIN o.invoice i " +
            "WHERE d.courierName = 'Steadfast' " +
            "AND d.deliveryStatus IN (ecommerce.enums.DeliveryStatus.PENDING, ecommerce.enums.DeliveryStatus.READY_FOR_PICKUP) " +
            "AND d.pickedUpAt IS NULL " +
            "ORDER BY d.requestedAt ASC")
    List<AwaitingPickupDto> findAwaitingPickup();

    /** Parcels still moving, least recently synced first (never-synced first), for the status sync job. */
    @Query("SELECT new ecommerce.dto.courier.SyncTargetDto(d.id, d.courierName, d.consignmentId, i.invoiceNumber) " +
            "FROM Delivery d JOIN d.order o LEFT JOIN o.invoice i " +
            "WHERE d.consignmentId IS NOT NULL AND d.courierName IS NOT NULL " +
            "AND d.deliveryStatus IN (ecommerce.enums.DeliveryStatus.PENDING, ecommerce.enums.DeliveryStatus.READY_FOR_PICKUP, ecommerce.enums.DeliveryStatus.IN_TRANSIT) " +
            "AND d.requestedAt > :since " +
            "ORDER BY CASE WHEN d.lastSyncedAt IS NULL THEN 0 ELSE 1 END, d.lastSyncedAt ASC")
    List<SyncTargetDto> findSyncTargets(@org.springframework.data.repository.query.Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT new ecommerce.dto.courier.SyncTargetDto(d.id, d.courierName, d.consignmentId, i.invoiceNumber) " +
            "FROM Delivery d JOIN d.order o LEFT JOIN o.invoice i WHERE o.id = :orderId")
    Optional<SyncTargetDto> findSyncTargetByOrderId(@org.springframework.data.repository.query.Param("orderId") Long orderId);
}
