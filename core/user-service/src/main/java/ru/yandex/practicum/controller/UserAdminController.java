package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.user.UserOperations;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.service.UserService;
import java.util.List;

@RestController
@RequestMapping(path = "/admin/users")
public class UserAdminController implements UserOperations {

    private final UserService userService;

    @Autowired
    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody @Valid UserDto userDto) {
        return userService.create(userDto);
    }

    @Override
    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable("id") long id) {
        return userService.getById(id);
    }

    @Override
    @GetMapping("/{id}")
    public UserShortDto getUserShort(@PathVariable("id") long userId) {
        return userService.getUserShort(userId);
    }

    @Override
    @GetMapping
    public List<UserDto> getUsers(@RequestParam(value = "ids", required = false) List<Long> usersIds,
                                  @PositiveOrZero @RequestParam(value = "from", defaultValue = "0") int from,
                                  @Positive @RequestParam(value = "size", defaultValue = "10") int size) {
        return userService.getAll(usersIds, PageRequest.of(from, size));
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("id") long id) {
        userService.delete(id);
    }
}