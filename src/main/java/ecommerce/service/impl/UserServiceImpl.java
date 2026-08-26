package ecommerce.service.impl;

import ecommerce.dto.UserDto;
import ecommerce.dto.admin.user.UserStatsDto;
import ecommerce.dto.pageResponse.UserResponse;
import ecommerce.entity.AccountState;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.BadRequestException;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.UserRepository;
import ecommerce.service.UserService;
import ecommerce.utils.DateTimeUtil;
import ecommerce.utils.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DateTimeUtil dateTimeUtil;
    private final ModelMapper mapper;
    private final TokenUtil tokenUtil;

    public UserDto add(UserDto userDto) {

        User user = mapToEntity(userDto);
        user.setCreatedAt(LocalDateTime.now());
        User newUser = userRepository.save(user);

        return mapToDto(newUser);
    }

    @Override
    public UserDto getById(HttpServletRequest servletRequest) {
        User userInfo = tokenUtil.extractUserInfo(servletRequest);
        User user = userRepository.findById(userInfo.getId()).orElseThrow(()->
                new ResourceNotFound("User","id", userInfo.getId()));
        return mapToDto(user);
    }

    @Override
    public UserResponse getAll(int pageNo, int pageSize, String sortBy) {

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<User> users = userRepository.findAll(pageable);

        List<User> content = users.getContent();
        List<UserDto> userList = content.stream().map(this::mapToDto).toList();

        return getUserResponse(users, userList);
    }

    @Override
    public UserDto update(UserDto userDto, HttpServletRequest servletRequest) {

        User userInfo = tokenUtil.extractUserInfo(servletRequest);
        User user = userRepository.findById(userInfo.getId()).orElseThrow(()->
                new ResourceNotFound("User", "id", userInfo.getId()));

        user.setName(userDto.getName());
        if (user.getEmail() == null && userDto.getEmail() != null) {
            Optional<User> byEmail = userRepository.findByEmail(userDto.getEmail());
            if (byEmail.isPresent()) {
                throw new BadRequestException("Email already exist. please change");
            }
            user.setEmail(userDto.getEmail());
        }

        if (user.getPhone() == null && userDto.getPhone() != null) {
            Optional<User> byPhone = userRepository.findByPhone(userDto.getPhone());
            if (byPhone.isPresent()) {
                throw new BadRequestException("Phone already exist. please change");
            }
            user.setPhone(userDto.getPhone());
        }
        user.setDob(userDto.getDob());
        user.setGender(userDto.getGender());
        User updateInfo = userRepository.save(user);
        return mapToDto(updateInfo);
    }

    @Override
    public UserResponse search(int pageNo, int pageSize, String sortBy, String query) {

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<User> listOfUser = userRepository.search(pageable, query);

        List<User> users = listOfUser.getContent();
        List<UserDto> content = users.stream().map((this::mapToDto)).toList();

        return getUserResponse(listOfUser, content);
    }

    @NotNull
    public UserResponse getUserResponse(Page<User> listOfUser, List<UserDto> content) {
        UserResponse response = new UserResponse();
        response.setContent(content);
        response.setPageNo(listOfUser.getNumber());
        response.setPageSize(listOfUser.getSize());
        response.setTotalPages(listOfUser.getTotalPages());
        response.setTotalElements(listOfUser.getTotalElements());
        response.setLast(listOfUser.isLast());

        return response;
    }

    public Map<String, Object> getAdminUserList(int pageNo, int pageSize, String search, String role, String status, String sortKey, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Map UI column keys to entity field names
        String sortField = "id";
        if ("name".equalsIgnoreCase(sortKey)) sortField = "name";
        if ("createdAt".equalsIgnoreCase(sortKey)) sortField = "createdAt";

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(direction, sortField));
        Specification<User> spec = UserSpecification.filterUsers(search, role, status);

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserDto> content = userPage.getContent().stream()
                .map(this::mapToDto)
                .toList();

        Map<String, Object> meta = new HashMap<>();
        meta.put("total", userPage.getTotalElements());
        meta.put("page", pageNo + 1); // Return 1-indexed page for UI
        meta.put("totalPages", userPage.getTotalPages());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("data", content);
        responseData.put("meta", meta);

        return responseData;
    }

    @Override
    public UserStatsDto getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByAccountState(AccountState.ACTIVE);
        long unverifiedUsers = userRepository.countByAccountState(AccountState.UNVERIFIED);
        long suspendedUsers = userRepository.countByAccountState(AccountState.SUSPENDED);

        // Calculate users registered since start of current month
        LocalDateTime firstDayOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        long newThisMonth = userRepository.countByCreatedAtAfter(firstDayOfMonth);

        // Calculate percentages safely to avoid division by zero
        double activePercentage = totalUsers > 0
                ? Math.round(((double) activeUsers / totalUsers * 100) * 10.0) / 10.0
                : 0.0;

        double suspendedPercentage = totalUsers > 0
                ? Math.round(((double) suspendedUsers / totalUsers * 100) * 10.0) / 10.0
                : 0.0;

        return UserStatsDto.builder()
                .totalUsers(totalUsers)
                .newThisMonth(newThisMonth)
                .activeUsers(activeUsers)
                .activePercentage(activePercentage)
                .unverifiedUsers(unverifiedUsers)
                .suspendedUsers(suspendedUsers)
                .suspendedPercentage(suspendedPercentage)
                .build();
    }

    public UserDto mapToDto(User user) {
        return mapper.map(user, UserDto.class);
    }

    public User mapToEntity(UserDto userDto) {
        return mapper.map(userDto, User.class);
    }
}
