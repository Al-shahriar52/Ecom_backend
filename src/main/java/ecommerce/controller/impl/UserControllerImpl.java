package ecommerce.controller.impl;

import ecommerce.config.JwtService;
import ecommerce.controller.UserController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.UserDto;
import ecommerce.dto.pageResponse.UserResponse;
import ecommerce.service.UserService;
import ecommerce.service.impl.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserControllerImpl implements UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @PostMapping("/add")
    public ResponseEntity<?> add(@Valid @RequestBody UserDto userDto) {

        UserDto user = userService.add(userDto);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/get/{userId}")
    public ResponseEntity<UserDto> getById(@PathVariable Long userId) {

        UserDto user = userService.getById(userId);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @Override
    @GetMapping("/get/all")
    public ResponseEntity<?> getAll(@RequestParam(defaultValue = "0") int pageNo,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(defaultValue = "id") String sortBy) {

        UserResponse  response = userService.getAll(pageNo, pageSize, sortBy);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @PutMapping("/update/{userId}")
    public ResponseEntity<?> update(@PathVariable Long userId, @Valid @RequestBody UserDto userDto) {

        UserDto updateUser = userService.update(userId, userDto);
        return new ResponseEntity<>(updateUser, HttpStatus.OK);
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(defaultValue = "0") int pageNo,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(defaultValue = "id") String sortBy,
                                    @RequestParam(defaultValue = "") String query) {

        UserResponse user = userService.search(pageNo, pageSize, sortBy, query);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            String jti = jwtService.extractJti(jwt);
            tokenBlacklistService.blacklistToken(jti);
            return new ResponseEntity<>(GenericResponseDto.success("Successfully logged out.", null, HttpStatus.OK.value()), HttpStatus.OK);
        }

        return new ResponseEntity<>(GenericResponseDto.error("Forbidden", "Invalid request.", HttpStatus.FORBIDDEN.value()), HttpStatus.FORBIDDEN);
    }

}
