package ecommerce.dto.courier;

import ecommerce.entity.PickupRequest;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * What the admin "pickup history" list needs. Returning the PickupRequest entity directly breaks JSON
 * serialization (its lazy "location" is a Hibernate proxy), so the list is mapped to this plain object instead.
 */
@Data
public class PickupHistoryDto {
    private Long id;
    private String locationLabel;
    private String address;
    private String contactNumber;
    private String note;
    private Integer estimQty;
    private String status;
    private String steadfastPickupId;
    private String courierMessage;
    private String createdBy;
    private LocalDateTime createdAt;

    public static PickupHistoryDto from(PickupRequest r) {
        PickupHistoryDto dto = new PickupHistoryDto();
        dto.setId(r.getId());
        dto.setLocationLabel(r.getLocationLabel());
        dto.setAddress(r.getAddress());
        dto.setContactNumber(r.getContactNumber());
        dto.setNote(r.getNote());
        dto.setEstimQty(r.getEstimQty());
        dto.setStatus(r.getStatus());
        dto.setSteadfastPickupId(r.getSteadfastPickupId());
        dto.setCourierMessage(r.getCourierMessage());
        dto.setCreatedBy(r.getCreatedBy());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}
