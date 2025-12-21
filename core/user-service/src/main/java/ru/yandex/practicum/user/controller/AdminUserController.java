package ru.yandex.practicum.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.interaction.dto.NewUserRequest;
import ru.yandex.practicum.interaction.dto.UserDto;
import ru.yandex.practicum.interaction.dto.UserShortDto;
import ru.yandex.practicum.interaction.dto.params.UserParamsAdmin;
import ru.yandex.practicum.user.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody @Valid NewUserRequest request) {
        log.info("AdminUserController - Creating new user: {}", request);
        return ResponseEntity.status(201).body(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAll(@ModelAttribute UserParamsAdmin param) {
        log.info("AdminUserController - Getting all users with params: {}", param);
        return ResponseEntity.ok(userService.getUsers(param));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserShortDto> getUserById(@PathVariable Long userId) {
        log.info("AdminUserController - Getting user by ID: {}", userId);
        return ResponseEntity.ok(userService.findUserById(userId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        log.info("AdminUserController - Deleting user: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}