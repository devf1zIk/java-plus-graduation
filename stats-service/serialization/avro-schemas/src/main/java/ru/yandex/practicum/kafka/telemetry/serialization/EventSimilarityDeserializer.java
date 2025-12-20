package ru.yandex.practicum.kafka.telemetry.serialization;

import ru.yandex.practicum.kafka.telemetry.deserialization.BaseAvroDeserializer;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;

public class EventSimilarityDeserializer extends BaseAvroDeserializer<EventSimilarityAvro> {
    public EventSimilarityDeserializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}
