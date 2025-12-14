package ru.yandex.practicum.client.user;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.user.UserShortDto;

public interface UserOperations {

    @GetMapping("/admin/users/{userId}")
    UserShortDto getUser(@PathVariable @NotNull Long userId);
}
