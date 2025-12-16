package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.user.NewUserRequestDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.mapper.UserMapper;
import ru.yandex.practicum.dto.user.UserFullDto;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<UserFullDto> getUsers(List<Long> ids, int from, int size) {
        log.info("Получение пользователей. IDs: {}, from: {}, size: {}", ids, from, size);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        Page<User> userPage = userRepository.findAllByIdIn(ids, pageable);
        return userPage.getContent().stream()
                .map(userMapper::toUserFullDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserShortDto getUser(Long userId) {
        return UserMapper.fromUserToUserShortDto(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден")));
    }

    public UserFullDto addUser(NewUserRequestDto newUserRequestDto) {
        Optional<User> userWithSameName = userRepository.findByName(newUserRequestDto.getName());
        if (userWithSameName.isPresent()) {
            throw new ConflictException("User " + newUserRequestDto.getName() + " уже существует!");
        }

        return UserMapper.toUserDtoFromUser(userRepository.save(UserMapper.toUserFromUserDto(newUserRequestDto)));
    }

    public void deleteUser(long userId) {
        userRepository.delete(getUserIfExist(userId));
    }

    public User getUserIfExist(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User c id" + userId + " не существует!"));
    }
}