package ru.yandex.practicum.client.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.user.UserShortDto;

public interface UserOperations {

    @GetMapping("/{userId}")
    UserShortDto getUser(@PathVariable("userId") Long userId);
}
