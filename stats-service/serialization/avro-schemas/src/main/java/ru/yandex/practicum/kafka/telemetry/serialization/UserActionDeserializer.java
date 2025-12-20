package ru.yandex.practicum.kafka.telemetry.serialization;

import ru.yandex.practicum.kafka.telemetry.deserialization.BaseAvroDeserializer;
import ru.yandex.practicum.stats.avro.UserActionAvro;

public class UserActionDeserializer extends BaseAvroDeserializer<UserActionAvro> {
    public UserActionDeserializer() {
        super(UserActionAvro.getClassSchema());
    }
}
