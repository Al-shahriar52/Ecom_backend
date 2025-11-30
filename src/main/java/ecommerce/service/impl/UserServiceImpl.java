package ecommerce.service.impl;

import ecommerce.dto.UserDto;
import ecommerce.dto.pageResponse.UserResponse;
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
import org.springframework.stereotype.Service;

import java.util.List;
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

        String formattedTime = dateTimeUtil.convert();
        user.setCreatedAt(formattedTime);
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

    public UserDto mapToDto(User user) {
        return mapper.map(user, UserDto.class);
    }

    public User mapToEntity(UserDto userDto) {
        return mapper.map(userDto, User.class);
    }
}
