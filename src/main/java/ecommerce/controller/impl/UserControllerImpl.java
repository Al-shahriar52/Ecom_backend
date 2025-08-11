package ecommerce.controller.impl;

import ecommerce.controller.UserController;
import ecommerce.dto.UserDto;
import ecommerce.dto.pageResponse.UserResponse;
import ecommerce.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserControllerImpl implements UserController {

    private final UserService userService;

    public UserControllerImpl(UserService userService) {
        this.userService = userService;
    }

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

}
