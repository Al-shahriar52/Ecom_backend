package ecommerce.controller;

import ecommerce.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface UserController {

    ResponseEntity<?> add(UserDto userDto);
    ResponseEntity<?> getById(HttpServletRequest servletRequest);
    ResponseEntity<?> getAll(int pageNo, int pageSize, String sortBy);
    ResponseEntity<?> update(UserDto userDto, HttpServletRequest servletRequest);
    ResponseEntity<?> search(int pageNo, int pageSize, String sortBy, String query);
    ResponseEntity<?> logout(HttpServletRequest request);
}
