package ru.yandex.practicum.handler;

import ru.yandex.practicum.stats.proto.UserActionProto;

public interface UserActionHandler {
    void handle(UserActionProto userActionProto);
}
