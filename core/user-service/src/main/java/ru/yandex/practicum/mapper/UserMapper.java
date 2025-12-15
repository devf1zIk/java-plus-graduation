package ru.yandex.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.model.User;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapper {
    public static UserDto toUserDtoFromUser(User user) {
        UserDto userDto = new UserDto(user.getEmail(), (long) -1, user.getName());
        if (user.getId() != null) {
            userDto.setId(user.getId());
        }

        return userDto;
    }

    public static UserShortDto fromUserToUserShortDto(User user) {
        UserShortDto userShortDto = new UserShortDto((long) -1, user.getName());
        if (user.getId() != null) {
            userShortDto.setId(user.getId());
        }

        return userShortDto;
    }

    public static User toUserFromUserDto(UserDto userDto) {
        return new User(userDto.getId(), userDto.getName(), userDto.getEmail());
    }
}