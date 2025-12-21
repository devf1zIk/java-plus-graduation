package ru.yandex.practicum.analyzer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.analyzer.model.EventSimilarity;
import ru.yandex.practicum.analyzer.repository.EventSimilarityRepository;
import ru.yandex.practicum.analyzer.service.EventSimilarityService;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSimilarityServiceImpl implements EventSimilarityService {

    private final EventSimilarityRepository repository;

    @Override
    public void handleEventSimilarity(EventSimilarityAvro message) {
        long eventA = message.getEventA();
        long eventB = message.getEventB();
        double score = message.getScore();

        log.info("[Analyzer] Processing event similarity: eventA={}, eventB={}, score={}",
                eventA, eventB, score);

        repository.findAll().stream()
                .filter(e -> (e.getEventA().equals(eventA) && e.getEventB().equals(eventB)) ||
                        (e.getEventA().equals(eventB) && e.getEventB().equals(eventA)))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            existing.setScore(score);
                            repository.save(existing);
                            log.info("[Analyzer] Updated similarity: eventA={}, eventB={}, score={}", eventA, eventB, score);
                        },
                        () -> {
                            EventSimilarity newRecord = EventSimilarity.builder()
                                    .eventA(eventA)
                                    .eventB(eventB)
                                    .score(score)
                                    .build();
                            repository.save(newRecord);
                            log.info("[Analyzer] Inserted new similarity: eventA={}, eventB={}, score={}", eventA, eventB, score);
                        }
                );
    }
}
