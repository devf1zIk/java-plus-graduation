package ru.yandex.practicum.handler;

import ru.yandex.practicum.stats.avro.EventSimilarityAvro;

public interface SimilarityHandler {

    void handle(EventSimilarityAvro eventSimilarityAvro);
}
