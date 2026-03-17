package ecommerce.enums;
public enum DeliveryStatus {
    PENDING,            // Waiting to be processed
    READY_FOR_PICKUP,   // Sent to Steadfast, waiting for rider
    IN_TRANSIT,         // Picked up by courier
    DELIVERED,          // Successfully handed to customer
    RETURNED,           // Customer rejected / failed delivery
    CANCELLED           // Cancelled before pickup
}