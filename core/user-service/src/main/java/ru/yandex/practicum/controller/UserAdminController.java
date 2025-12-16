package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.user.UserFullDto;
import ru.yandex.practicum.service.UserService;
import java.util.List;

@RestController
@RequestMapping(path = "/admin/users")
public class UserAdminController {

    private final UserService userService;

    @Autowired
    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserFullDto addUser(@RequestBody @Valid UserFullDto userFullDto) {
        return userService.addUser(userFullDto);
    }

    @GetMapping("/{id}")
    public UserFullDto getUser(@PathVariable("id") Integer id) {
        return userService.getUser(id);
    }

    @GetMapping
    public List<UserFullDto> getUsers(@RequestParam(value = "ids", required = false) List<Long> usersIds,
                                      @PositiveOrZero @RequestParam(value = "from", defaultValue = "0") int from,
                                      @Positive @RequestParam(value = "size", defaultValue = "10") int size) {
        return userService.getUsers(usersIds, PageRequest.of(from, size));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("id") Integer id) {
        userService.deleteUser(id);
    }
}