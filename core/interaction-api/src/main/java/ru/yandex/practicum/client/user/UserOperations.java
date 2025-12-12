package ru.yandex.practicum.client.user;

import org.springframework.web.bind.annotation.GetMapping;
import ru.yandex.practicum.dto.user.UserShortDto;

public interface UserOperations {

    @GetMapping("/admin/users/{id}")
    UserShortDto getUser(long userId);
}
