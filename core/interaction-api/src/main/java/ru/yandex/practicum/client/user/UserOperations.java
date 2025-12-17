package ru.yandex.practicum.client.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.dto.user.UserShortDto;
import java.util.List;
import java.util.Map;

public interface UserOperations {

    @GetMapping("/{userId}")
    UserShortDto getUser(@PathVariable("userId") Long userId);

    @PostMapping("/batch")
    Map<Long, UserShortDto> getUsersBatch(@RequestBody List<Long> userIds);
}
