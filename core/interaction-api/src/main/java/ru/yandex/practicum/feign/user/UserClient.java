package ru.yandex.practicum.feign.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.user.UserShortDto;
import java.util.List;

@FeignClient(name = "USER-SERVICE", path = "/internal/users", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/{userId}")
    UserShortDto getById(@PathVariable("userId") Long userId);

    @GetMapping("/batch")
    List<UserShortDto> getByIds(@RequestParam("ids") List<Long> ids);
}
