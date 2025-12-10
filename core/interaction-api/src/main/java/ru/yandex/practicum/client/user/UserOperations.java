package ru.yandex.practicum.client.user;

import ru.yandex.practicum.dto.user.UserDto;
import java.util.List;

public interface UserOperations {

    UserDto create(UserDto userDto);

    UserDto getUser(long userId);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(long userId);
}
