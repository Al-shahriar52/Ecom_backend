package ecommerce.service.impl;

import ecommerce.repository.CartItemRepository;
import ecommerce.repository.CartRepository;
import ecommerce.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestCleanupService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Scheduled(cron = "0 0 3 * * *") // daily at 3 AM
    @Transactional
    public void cleanupStaleGuests() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        // Format to match your DB string representation (adjust pattern if needed)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String cutoffString = cutoff.format(formatter);
        List<Long> staleGuestIds = userRepository.findStaleGuestUserIds(cutoffString);

        if (staleGuestIds.isEmpty()) {
            log.info("Guest cleanup: nothing to delete");
            return;
        }

        // order matters: children before parent, to satisfy FK constraints
        cartItemRepository.deleteByCartUserIdIn(staleGuestIds);
        cartRepository.deleteByUserIdIn(staleGuestIds);
        userRepository.deleteAllByIdInBatch(staleGuestIds);

        log.info("Guest cleanup: deleted {} stale guest users", staleGuestIds.size());
    }
}