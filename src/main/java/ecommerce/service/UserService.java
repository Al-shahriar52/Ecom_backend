package ecommerce.service;

import ecommerce.dto.UserDto;
import ecommerce.dto.admin.user.UserStatsDto;
import ecommerce.dto.pageResponse.UserResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface UserService {

    UserDto add(UserDto userDto);
    UserDto getById(HttpServletRequest servletRequest);
    UserResponse getAll(int pageNo, int pageSize, String sortBY);
    UserDto update(UserDto userDto, HttpServletRequest servletRequest);
    UserResponse search(int pageNo, int pageSize, String sortBy, String query);
    Map<String, Object> getAdminUserList(int pageNo, int pageSize, String search, String role, String status, String sortKey, String sortDir);
    UserStatsDto getUserStats();
}
