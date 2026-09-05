package ecommerce.service.impl;

import ecommerce.dto.UserDto;
import ecommerce.dto.admin.user.BulkImportResponseDto;
import ecommerce.dto.admin.user.UserDetailsResponseDto;
import ecommerce.dto.admin.user.UserRequestDto;
import ecommerce.entity.*;
import ecommerce.enums.OrderStatus;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.repository.*;
import ecommerce.service.ActivityService;
import ecommerce.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final CartRepository cartRepository;

    private static final String TEMP_PWD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public UserDto createUser(UserRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("User already exists with email: " + request.getEmail());
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("User already exists with phone: " + request.getPhone());
        }

        User user = new User();
        mapRequestToEntity(request, user);

        // Generate temporary password
        String rawTempPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(rawTempPassword));

        // Default initial account state
        user.setAccountState(AccountState.UNVERIFIED);
        user.setStatus(true);

        User savedUser = userRepository.save(user);

        activityService.logActivity(savedUser.getId(), "Account created by Admin");

        // Dispatch email notification with credentials
        emailService.sendTemporaryPassword(savedUser.getEmail(), savedUser.getName(), rawTempPassword);

        return mapEntityToDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserRequestDto request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));

        if (!existingUser.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }

        mapRequestToEntity(request, existingUser);

        User updatedUser = userRepository.save(existingUser);
        activityService.logActivity(updatedUser.getId(), "Account updated by Admin");
        return mapEntityToDto(updatedUser);
    }

    @Override
    @Transactional
    public BulkImportResponseDto importUsersFromCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalProcessed = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return new BulkImportResponseDto(0, 0, 0, List.of("Uploaded CSV file is empty"));
            }

            String[] headers = headerLine.toLowerCase().split(",");
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim(), i);
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                totalProcessed++;

                String[] values = line.split(",", -1);

                try {
                    String name = getColumnValue(values, headerMap, "name");
                    String email = getColumnValue(values, headerMap, "email");
                    String phone = getColumnValue(values, headerMap, "phone");
                    String roleStr = getColumnValue(values, headerMap, "role");
                    String genderStr = getColumnValue(values, headerMap, "gender");

                    if (email.isBlank()) {
                        errors.add("Row " + lineNumber + ": Email column is required");
                        continue;
                    }

                    if (userRepository.existsByEmail(email)) {
                        errors.add("Row " + lineNumber + ": Duplicate email (" + email + ")");
                        continue;
                    }

                    User user = new User();
                    user.setName(name.isBlank() ? "Imported User" : name);
                    user.setEmail(email);
                    user.setPhone(phone.isBlank() ? null : phone);
                    user.setRoles(parseRoles(roleStr));

                    if (!genderStr.isBlank()) {
                        try {
                            user.setGender(Gender.valueOf(genderStr.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {}
                    }

                    user.setAccountState(AccountState.UNVERIFIED);
                    user.setStatus(false);

                    String rawTempPassword = generateTemporaryPassword();
                    user.setPassword(passwordEncoder.encode(rawTempPassword));

                    User savedUser = userRepository.save(user);

                    emailService.sendTemporaryPassword(savedUser.getEmail(), savedUser.getName(), rawTempPassword);
                    activityService.logActivity(savedUser.getId(), "Account created by Admin");
                    successCount++;

                } catch (Exception e) {
                    errors.add("Row " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("CSV Import Error: " + e.getMessage());
        }

        return new BulkImportResponseDto(totalProcessed, successCount, errors.size(), errors);
    }

    // --- Helpers ---

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(TEMP_PWD_CHARS.charAt(RANDOM.nextInt(TEMP_PWD_CHARS.length())));
        }
        return sb.toString();
    }

    private String getColumnValue(String[] values, Map<String, Integer> headerMap, String key) {
        Integer index = headerMap.get(key);
        return (index != null && index < values.length) ? values[index].trim() : "";
    }

    private Set<Role> parseRoles(String roleInput) {
        Set<Role> roles = new HashSet<>();

        if (roleInput != null && !roleInput.isBlank()) {
            try {
                // Directly converts "ADMIN" -> Role.ADMIN and "USER" -> Role.USER
                roles.add(Role.valueOf(roleInput.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                roles.add(Role.USER); // Default fallback if an invalid string is passed
            }
        } else {
            roles.add(Role.USER);
        }

        return roles;
    }

    private void mapRequestToEntity(UserRequestDto dto, User entity) {
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setGender(dto.getGender());
        entity.setRoles(parseRoles(dto.getRole()));
    }

    private UserDto mapEntityToDto(User entity) {
        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setGender(entity.getGender());
        dto.setDob(entity.getDob());
        dto.setRoles(entity.getRoles());
        dto.setAccountState(entity.getAccountState());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setOrders(entity.getOrders());
        dto.setAvatarVariant(entity.getAvatarVariant());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailsResponseDto getUserDetails(Long id) {
        // 1. Fetch the user
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));

        UserDetailsResponseDto response = new UserDetailsResponseDto();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setAvatarVariant(user.getAvatarVariant());

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            response.setRole(user.getRoles().iterator().next().name());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        // 2. Fetch real Orders & Process Stats + History
        List<Order> userOrders = orderRepository.findByUserId(id);
        UserDetailsResponseDto.OrderStatsDto stats = new UserDetailsResponseDto.OrderStatsDto();
        List<UserDetailsResponseDto.OrderHistoryDto> historyList = new ArrayList<>();

        double totalSpent = 0.0;
        int confirmed = 0; // Added confirmed counter
        int delivered = 0;
        int pending = 0;
        int cancelled = 0;

        for (Order order : userOrders) {
            if (order.getOrderStatus() == OrderStatus.DELIVERED) {
                delivered++;
                totalSpent += (order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);
            } else if (order.getOrderStatus() == OrderStatus.CONFIRMED) {
                confirmed++;
            } else if (order.getOrderStatus() == OrderStatus.PENDING) {
                pending++;
            } else if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                cancelled++;
            }

            UserDetailsResponseDto.OrderHistoryDto historyDto = new UserDetailsResponseDto.OrderHistoryDto();
            historyDto.setId("ORD-" + order.getId());
            historyDto.setDate(order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : "N/A");
            historyDto.setItems(order.getOrderItems() != null ? order.getOrderItems().size() : 0);
            historyDto.setAmount(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);
            historyDto.setStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : "UNKNOWN");

            historyList.add(historyDto);
        }

        stats.setTotalSpent(totalSpent);
        stats.setConfirmed(confirmed); // Set the confirmed count here
        stats.setDelivered(delivered);
        stats.setPending(pending);
        stats.setCancelled(cancelled);

        response.setOrderStats(stats);
        response.setOrderHistory(historyList);

        // 3. Fetch real Addresses (Now mapping the new isDefault field!)
        List<Address> userAddresses = addressRepository.findByUserId(id);
        List<UserDetailsResponseDto.AddressDto> addressDtos = userAddresses.stream().map(addr -> {
            UserDetailsResponseDto.AddressDto dto = new UserDetailsResponseDto.AddressDto();
            dto.setId(addr.getId());
            dto.setAddressType(addr.getAddressType() != null ? addr.getAddressType() : AddressType.HOME);
            dto.setDefault(addr.isDefault()); // Mapped real value here
            dto.setAddress(addr.getAddress());
            dto.setArea(addr.getArea());
            dto.setCity(addr.getCity());
            return dto;
        }).collect(Collectors.toList());

        response.setAddresses(addressDtos);

        // 4. Fetch real Activity Log
        List<Activity> userActivities = activityRepository.findTop10ByUserIdOrderByCreatedAtDesc(id);
        List<UserDetailsResponseDto.ActivityDto> activityDtos = userActivities.stream().map(act -> {
            UserDetailsResponseDto.ActivityDto actDto = new UserDetailsResponseDto.ActivityDto();
            actDto.setA(act.getActionText());
            actDto.setT(act.getCreatedAt() != null ? act.getCreatedAt().format(formatter) : "Recently");
            return actDto;
        }).collect(Collectors.toList());

        response.setActivity(activityDtos);

        return response;
    }

    @Override
    @Transactional
    public void anonymizeAndDeleteUser(Long id) {
        // 1. Fetch user
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));

        // 3. Anonymize Personally Identifiable Information (PII)
        user.setName("Deleted User");
        user.setEmail("deleted_user_" + id + "@deleted.local");
        user.setPhone(null);
        user.setPassword("DELETED_ACCOUNT_" + System.currentTimeMillis());
        user.setDob(null);
        user.setGender(null);
        user.setOtp(null);
        user.setOtpExpiry(null);

        // 4. Update status and account state
        user.setStatus(false);
        user.setAccountState(AccountState.SUSPENDED);

        // 5. Persist the anonymized entity
        userRepository.save(user);
    }

    @Transactional
    public void toggleUserSuspension(Long id, boolean suspend) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));

        if (suspend) {
            user.setStatus(false); // Disables login check in Spring Security
            user.setAccountState(AccountState.SUSPENDED);
        } else {
            user.setStatus(true); // Restores active access
            user.setAccountState(AccountState.ACTIVE);
        }

        userRepository.save(user);
    }
}