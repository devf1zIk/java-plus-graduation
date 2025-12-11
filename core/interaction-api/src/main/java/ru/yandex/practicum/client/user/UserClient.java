package ru.yandex.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.user.UserShortDto;

@FeignClient(name = "user-service", path = "/admin/users", contextId = "userAdminClient", fallback = UserClientFallback.class)
public interface UserClient extends UserOperations {

    @GetMapping("/{id}")
    UserShortDto getUserShort(@PathVariable("id") long id);
}
