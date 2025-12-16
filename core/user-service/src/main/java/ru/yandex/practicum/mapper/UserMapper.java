package ru.yandex.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.user.UserFullDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapper {
    public static UserFullDto toUserDtoFromUser(User user) {
        UserFullDto userFullDto = new UserFullDto(user.getEmail(), -1, user.getName());
        if (user.getId() != null) {
            userFullDto.setId(user.getId());
        }

        return userFullDto;
    }

    public static UserShortDto fromUserToUserShortDto(User user) {
        UserShortDto userShortDto = new UserShortDto(-1, user.getName());
        if (user.getId() != null) {
            userShortDto.setId(user.getId());
        }

        return userShortDto;
    }

    public static User toUserFromUserDto(UserFullDto userFullDto) {
        return new User(userFullDto.getId(), userFullDto.getName(), userFullDto.getEmail());
    }
}