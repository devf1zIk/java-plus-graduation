package ru.yandex.practicum.user.service;


import ru.yandex.practicum.interaction.dto.NewUserRequest;
import ru.yandex.practicum.interaction.dto.UserDto;
import ru.yandex.practicum.interaction.dto.UserShortDto;
import ru.yandex.practicum.interaction.dto.params.UserParamsAdmin;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest request);

    List<UserDto> getUsers(UserParamsAdmin param);

    UserShortDto findUserById(Long userId);

    void deleteUser(Long userId);

}