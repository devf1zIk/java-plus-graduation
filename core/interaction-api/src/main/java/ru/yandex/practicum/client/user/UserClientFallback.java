package ru.yandex.practicum.client.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.user.UserShortDto;

@Slf4j
@Component
public class UserClientFallback implements UserOperations{

    @Override
    public UserShortDto getUser(Long userId) {
        log.error("User service unavailable → getById: {}", userId);
        return null;
    }
}
