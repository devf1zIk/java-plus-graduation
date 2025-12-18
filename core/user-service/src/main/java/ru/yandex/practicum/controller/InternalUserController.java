package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.mapper.UserMapper;
import ru.yandex.practicum.service.UserService;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public UserShortDto getById(@PathVariable Long userId) {
        return UserMapper.toUserShortDto(userService.getUserIfExist(userId));
    }

    @GetMapping("/batch")
    public List<UserShortDto> getByIds(@RequestParam List<Long> ids) {
        return userService.getShortByIds(ids);
    }
}
