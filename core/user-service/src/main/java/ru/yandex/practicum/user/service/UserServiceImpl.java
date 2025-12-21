package ru.yandex.practicum.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.dto.NewUserRequest;
import ru.yandex.practicum.interaction.dto.UserDto;
import ru.yandex.practicum.interaction.dto.UserShortDto;
import ru.yandex.practicum.interaction.dto.params.UserParamsAdmin;
import ru.yandex.practicum.interaction.exception.ConflictException;
import ru.yandex.practicum.interaction.exception.NotFoundException;
import ru.yandex.practicum.user.mapper.UserMapper;
import ru.yandex.practicum.user.model.User;
import ru.yandex.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with this email already exists");
        }
        User user = UserMapper.toEntity(request);
        return UserMapper.toDto(userRepository.save(user));
    }

    @Override
    public List<UserDto> getUsers(UserParamsAdmin param) {
        List<User> users = (param.getIds() != null && !param.getIds().isEmpty())
                ? userRepository.findAllByIdIn(param.getIds())
                : userRepository.findAll(PageRequest.of(param.getFrom() / param.getSize(),
                param.getSize())).getContent();

        return users.stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserShortDto findUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("The user with id: " + userId + " not found!"));
        return UserMapper.toShortDto(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("The user with id: " + userId + " not found!"));
        userRepository.deleteById(userId);
    }
}