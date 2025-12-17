package ru.yandex.practicum.client.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.exception.model.ServiceUnavailableException;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
public class UserClientFallback implements UserOperations{

    @Override
    public UserShortDto getUser(Long userId) {
        log.error("User service unavailable → getUser: {}", userId);
        return null;
    }

    @Override
    public Map<Long, UserShortDto> getUsersBatch(List<Long> userIds) {
        log.error("User service unavailable → getUsersBatch, userIds={}", userIds);
        throw new ServiceUnavailableException("User service is unavailable");
    }
}
