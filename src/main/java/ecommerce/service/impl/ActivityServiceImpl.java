package ecommerce.service.impl;

import ecommerce.entity.Activity;
import ecommerce.repository.ActivityRepository;
import ecommerce.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;

    @Override
    @Async // Tells Spring to run this in a background thread
    @Transactional
    public void logActivity(Long userId, String actionText) {
        try {
            Activity activity = new Activity();
            activity.setUserId(userId);
            activity.setActionText(actionText);
            activity.setCreatedAt(LocalDateTime.now());

            activityRepository.save(activity);
        } catch (Exception e) {
            // If saving fails, it prints an error to your console instead of crashing the app
            log.error("Failed to save activity log for user {}: {}", userId, e.getMessage());
        }
    }
}