package ru.yandex.practicum.interaction.fallback;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.interaction.client.UserClient;
import ru.yandex.practicum.interaction.dto.UserDto;
import ru.yandex.practicum.interaction.dto.UserShortDto;
import ru.yandex.practicum.interaction.dto.params.UserParamsAdmin;

import java.util.Collections;
import java.util.List;

@Slf4j
public class UserFallback implements UserClient {

    @Override
    public List<UserDto> getAll(UserParamsAdmin param) {
        log.warn("[UserClient#getAll] Fallback triggered: getAll users failed. Params: {}", param);
        return Collections.emptyList();
    }

    @Override
    public UserShortDto getUserById(Long userId) {
        log.warn("[UserClient#getUserById] Fallback triggered: get user by ID {} failed.", userId);
        return null;
    }
}
