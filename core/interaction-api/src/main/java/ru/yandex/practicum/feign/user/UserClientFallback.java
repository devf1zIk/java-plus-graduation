package ru.yandex.practicum.feign.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.user.UserShortDto;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserClientFallback implements UserClient {

    private static final String UNKNOWN = "Unknown User";

    @Override
    public UserShortDto getById(Long userId) {
        return new UserShortDto(userId, UNKNOWN + " (" + userId + ")");
    }

    @Override
    public List<UserShortDto> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(id -> new UserShortDto(id, UNKNOWN + " (" + id + ")"))
                .collect(Collectors.toList());
    }
}