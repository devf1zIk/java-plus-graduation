package ru.yandex.practicum.client.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.user.UserShortDto;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserClientFallback implements UserOperations{

    @Override
    public UserShortDto getUserShortById(Long userId) {
        log.error("User service unavailable → getUserShortById: {}", userId);
        return null;
    }

    @Override
    public List<UserShortDto> getAllUsersShort(List<Long> ids) {
        log.error("User service unavailable → getAllUsersShort: {}", ids);
        return Collections.emptyList();
    }
}
