package ru.yandex.practicum.client.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserClientFallback implements UserOperations{

    @Override
    public UserDto create(UserDto userDto) {
        log.error("User service unavailable → create: {}",userDto.getId());
        return null;
    }

    @Override
    public UserDto getUser(long userId) {
        log.error("User service unavailable → getById: {}", userId);
        return null;
    }

    @Override
    public UserShortDto getUserShort(long userId) {
        log.error("UserShort service unavailable → getById: {}", userId);
        return null;
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.error("User service unavailable → getAll: {}",ids);
        return Collections.emptyList();
    }

    @Override
    public void deleteUser(long userId) {
        log.error("User service unavailable → delete: {}", userId);
    }
}
