package ru.yandex.practicum.client.user;

import ru.yandex.practicum.dto.user.UserShortDto;

public interface UserOperations {

    UserShortDto getUser(long userId);
}
