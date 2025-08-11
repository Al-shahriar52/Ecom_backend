package ecommerce.service.impl;

import ecommerce.dto.UserDto;
import ecommerce.dto.pageResponse.UserResponse;
import ecommerce.entity.User;
import ecommerce.exceptionHandling.ResourceNotFound;
import ecommerce.repository.UserRepository;
import ecommerce.service.UserService;
import ecommerce.utils.DateTimeUtil;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DateTimeUtil dateTimeUtil;
    private final ModelMapper mapper;

    public UserServiceImpl(UserRepository userRepository, DateTimeUtil dateTimeUtil, ModelMapper mapper) {
        this.userRepository = userRepository;
        this.dateTimeUtil = dateTimeUtil;
        this.mapper = mapper;
    }

    public UserDto add(UserDto userDto) {

        User user = mapToEntity(userDto);

        String formattedTime = dateTimeUtil.convert();
        user.setCreatedAt(formattedTime);
        User newUser = userRepository.save(user);

        return mapToDto(newUser);
    }

    @Override
    public UserDto getById(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(()->
                new ResourceNotFound("User","id",userId));

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
    public UserDto update(Long userId, UserDto userDto) {

        User user = userRepository.findById(userId).orElseThrow(()->
                new ResourceNotFound("User", "id", userId));

        user.setName(userDto.getName());
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
