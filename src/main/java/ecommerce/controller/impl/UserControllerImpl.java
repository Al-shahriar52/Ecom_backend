package ecommerce.controller.impl;

import ecommerce.config.JwtService;
import ecommerce.controller.UserController;
import ecommerce.dto.GenericResponseDto;
import ecommerce.dto.LoginResponse;
import ecommerce.dto.UserDto;
import ecommerce.dto.pageResponse.UserResponse;
import ecommerce.service.AuthService;
import ecommerce.service.UserService;
import ecommerce.service.impl.TokenBlacklistService;
import ecommerce.utils.TokenUtil;
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
    private final AuthService authService;
    private final TokenUtil tokenUtil;

    @Override
    @PostMapping("/add")
    public ResponseEntity<?> add(@Valid @RequestBody UserDto userDto) {

        UserDto user = userService.add(userDto);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @Override
    @GetMapping("/get")
    public ResponseEntity<?> getById(HttpServletRequest servletRequest) {
        UserDto user = userService.getById(servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Fetch successfully", user, HttpStatus.OK.value()), HttpStatus.OK);
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
    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody @Valid UserDto userDto, HttpServletRequest servletRequest) {
        UserDto updateUser = userService.update(userDto, servletRequest);
        return new ResponseEntity<>(GenericResponseDto.success("Account updated successfully", updateUser, HttpStatus.OK.value()), HttpStatus.OK);
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
    @Override
    public ResponseEntity<?> logout(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            String jti = jwtService.extractJti(jwt);
            tokenBlacklistService.blacklistToken(jti);
            return new ResponseEntity<>(GenericResponseDto.success("Successfully logged out.", null, HttpStatus.OK.value()), HttpStatus.OK);
        }

        return new ResponseEntity<>(GenericResponseDto.error("Forbidden", "Access token required.", HttpStatus.FORBIDDEN.value()), HttpStatus.FORBIDDEN);
    }

    @PostMapping("/refreshAccessToken")
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request
    ) {
        LoginResponse response = authService.refreshToken(request);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return new ResponseEntity<>(GenericResponseDto.success("Access token generated", response, HttpStatus.OK.value()), HttpStatus.OK);
    }
}
