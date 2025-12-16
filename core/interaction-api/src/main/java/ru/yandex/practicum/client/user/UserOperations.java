package ru.yandex.practicum.client.user;

import feign.FeignException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.user.UserShortDto;
import java.util.List;

public interface UserOperations {

    @GetMapping("/{userId}")
    UserShortDto getUserShortById(@PathVariable("userId") Long userId) throws FeignException;

    @GetMapping("/list")
    List<UserShortDto> getAllUsersShort(@RequestParam("ids") List<Long> ids) throws FeignException;
}
