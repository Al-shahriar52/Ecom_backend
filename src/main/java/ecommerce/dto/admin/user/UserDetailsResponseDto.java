package ecommerce.dto.admin.user;

import ecommerce.entity.Address;
import ecommerce.entity.AddressType;
import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class UserDetailsResponseDto {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role; 
    private boolean status;
    private String avatarVariant;
    
    // Nested objects required by the frontend tabs
    private OrderStatsDto orderStats;
    private List<AddressDto> addresses;
    private List<OrderHistoryDto> orderHistory;
    private List<ActivityDto> activity;

    @Data
    public static class OrderStatsDto {
        private double totalSpent;
        private int delivered;
        private int pending;
        private int cancelled;
    }

    @Data
    public static class AddressDto {
        private Long id;
        private AddressType addressType; // e.g., "Home", "Billing"
        private boolean isDefault;
        private String address;
        private String area;
        private String city;
    }

    @Data
    public static class OrderHistoryDto {
        private String id; // The frontend uses this as a string, e.g., "ORD-123"
        private String date;
        private int items;
        private double amount;
        private String status; // e.g., "Delivered", "Pending"
    }

    @Data
    public static class ActivityDto {
        private String a; // Action text
        private String t; // Time string
    }
}