package ru.yandex.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.user.UserDto;
import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users", contextId = "userAdminClient", fallback = UserClientFallback.class)
public interface UserClient extends UserOperations {

    @Override
    @PostMapping
    UserDto create(@RequestBody UserDto userDto);

    @Override
    @GetMapping("/{id}")
    UserDto getUser(@PathVariable("id") long id);

    @Override
    @GetMapping
    List<UserDto> getUsers(@RequestParam(value = "ids", required = false) List<Long> ids,
                           @RequestParam("from") int from,
                           @RequestParam("size") int size);

    @Override
    @DeleteMapping("/{id}")
    void deleteUser(@PathVariable("id") long id);
}
