package ru.practicum.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.exception.model.ConflictException;
import ru.practicum.exception.model.NotFoundException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDto> getAll(List<Long> usersId, Pageable pageable) {
        if (usersId == null) {
            return userRepository.findAll(pageable).stream()
                    .map(UserMapper::toUserDtoFromUser).toList();
        } else {
            return userRepository.findAllByIdIn(usersId, pageable).stream()
                    .map(UserMapper::toUserDtoFromUser).toList();
        }
    }

    public UserDto getById(long userId) {
        return UserMapper.toUserDtoFromUser(getUserIfExist(userId));
    }

    public UserDto create(UserDto userDto) {
        Optional<User> userWithSameName = userRepository.findByName(userDto.getName());
        if (userWithSameName.isPresent()) {
            throw new ConflictException("User " + userDto.getName() + " уже существует!");
        }

        return UserMapper.toUserDtoFromUser(userRepository.save(UserMapper.toUserFromUserDto(userDto)));
    }

    public void delete(long userId) {
        userRepository.delete(getUserIfExist(userId));
    }

    public User getUserIfExist(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User c id" + userId + " не существует!"));
    }
}