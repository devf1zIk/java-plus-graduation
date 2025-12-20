package ru.yandex.practicum.handler;

import ru.yandex.practicum.stats.avro.UserActionAvro;

public interface UserActionHandler {

    void handle(UserActionAvro userActionAvro);
}
