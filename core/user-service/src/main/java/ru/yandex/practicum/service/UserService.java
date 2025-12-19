package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.user.NewUserRequestDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.mapper.UserMapper;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.UserRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto create(NewUserRequestDto requestDto) {
        userRepository.findByEmail(requestDto.getEmail())
                .ifPresent(u -> { throw new ConflictException("Email " + requestDto.getEmail() + " уже используется"); });

        User user = UserMapper.toUser(requestDto);
        User saved = userRepository.save(user);
        return UserMapper.toUserDto(saved);
    }

    public List<UserDto> getAll(List<Long> ids, Pageable pageable) {
        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(pageable)
                    .map(UserMapper::toUserDto)
                    .toList();
        }
        return userRepository.findAllByIdIn(ids, pageable)
                .map(UserMapper::toUserDto)
                .toList();
    }

    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(getUserIfExist(userId));
    }

    public void delete(Long userId) {
        userRepository.delete(getUserIfExist(userId));
    }

    public User getUserIfExist(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User с id " + userId + " не существует"));
    }

    public List<UserShortDto> getShortByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .map(UserMapper::toUserShortDto)
                .toList();
    }
}