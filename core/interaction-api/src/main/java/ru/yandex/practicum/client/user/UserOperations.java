package ru.yandex.practicum.client.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.user.UserShortDto;

public interface UserOperations {

    @GetMapping("/admin/users/{id}")
    UserShortDto getUser(@PathVariable("id") long userId);
}
