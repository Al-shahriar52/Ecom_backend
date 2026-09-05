package ecommerce.service;

import ecommerce.dto.UserDto;
import ecommerce.dto.admin.user.BulkImportResponseDto;
import ecommerce.dto.admin.user.UserDetailsResponseDto;
import ecommerce.dto.admin.user.UserRequestDto;
import org.springframework.web.multipart.MultipartFile;

public interface AdminUserService {

    UserDto createUser(UserRequestDto request);
    UserDto updateUser(Long id, UserRequestDto request);
    BulkImportResponseDto importUsersFromCsv(MultipartFile file);
    UserDetailsResponseDto getUserDetails(Long id);
}
