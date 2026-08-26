package ecommerce.dto.admin.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {
    private long totalUsers;
    private long newThisMonth;
    private long activeUsers;
    private double activePercentage;
    private long unverifiedUsers;
    private long suspendedUsers;
    private double suspendedPercentage;
}