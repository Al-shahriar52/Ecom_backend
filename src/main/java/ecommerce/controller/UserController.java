package ecommerce.controller;

import ecommerce.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface UserController {

    ResponseEntity<?> add(UserDto userDto);
    ResponseEntity<UserDto> getById(Long userId);
    ResponseEntity<?> getAll(int pageNo, int pageSize, String sortBy);
    ResponseEntity<?> update(Long userId, UserDto userDto);
    ResponseEntity<?> search(int pageNo, int pageSize, String sortBy, String query);
    ResponseEntity<?> logout(HttpServletRequest request);
}
