package ru.yandex.practicum.interaction.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.interaction.config.FeignConfig;
import ru.yandex.practicum.interaction.dto.UserDto;
import ru.yandex.practicum.interaction.dto.UserShortDto;
import ru.yandex.practicum.interaction.dto.params.UserParamsAdmin;
import ru.yandex.practicum.interaction.fallback.UserFallback;

import java.util.List;

@FeignClient(name = "user-service",
        path = "/admin/users",
        configuration = FeignConfig.class,
        fallback = UserFallback.class)
public interface UserClient {

    @GetMapping
    List<UserDto> getAll(@SpringQueryMap UserParamsAdmin param);

    @GetMapping("/{userId}")
    UserShortDto getUserById(@PathVariable Long userId);

}
