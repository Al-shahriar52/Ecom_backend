package ecommerce.service;

import ecommerce.dto.UserDto;
import ecommerce.dto.pageResponse.UserResponse;

public interface UserService {

    UserDto add(UserDto userDto);
    UserDto getById(Long userId);
    UserResponse getAll(int pageNo, int pageSize, String sortBY);
    UserDto update(Long userId, UserDto userDto);
    UserResponse search(int pageNo, int pageSize, String sortBy, String query);
}
