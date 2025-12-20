package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.entity.EventSimilarity;
import ru.yandex.practicum.services.EventSimilarityService;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;

@Component
@RequiredArgsConstructor
public class SimilarityHandlerImpl implements SimilarityHandler {
    private final EventSimilarityService eventSimilarityService;

    @Override
    public void handle(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarity = map(eventSimilarityAvro);
        eventSimilarityService.save(eventSimilarity);
    }

    EventSimilarity map(EventSimilarityAvro eventSimilarityAvro) {
        return EventSimilarity.builder()
                .eventA(eventSimilarityAvro.getEventA())
                .eventB(eventSimilarityAvro.getEventB())
                .score(eventSimilarityAvro.getScore())
                .build();
    }
}
