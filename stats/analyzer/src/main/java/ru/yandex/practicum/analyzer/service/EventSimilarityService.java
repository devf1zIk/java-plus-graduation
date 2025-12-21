package ru.yandex.practicum.analyzer.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface EventSimilarityService {

    void handleEventSimilarity(EventSimilarityAvro message);
}
