package ecommerce.service;

public interface ActivityService {
    /**
     * Logs a new activity for a specific user.
     * 
     * @param userId The ID of the user performing the action.
     * @param actionText A description of what the user did (e.g., "Account created").
     */
    void logActivity(Long userId, String actionText);
}