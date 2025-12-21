package ru.yandex.practicum.analyzer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.analyzer.model.UserEventWeight;
import ru.yandex.practicum.analyzer.repository.UserEventWeightRepository;
import ru.yandex.practicum.analyzer.service.UserActionService;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActionServiceImpl implements UserActionService {

    private final UserEventWeightRepository repository;

    @Override
    public void handleUserAction(UserActionAvro message) {
        long userId = message.getUserId();
        long eventId = message.getEventId();
        double weight = mapActionToWeight(message);

        log.info("[Analyzer] Processing user action: userId={}, eventId={}, actionType={}, weight={}",
                userId, eventId, message.getActionType(), weight);

        repository.findAll().stream()
                .filter(e -> e.getUserId().equals(userId) && e.getEventId().equals(eventId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            if (weight > existing.getWeight()) {
                                existing.setWeight(weight);
                                existing.setTimestamp(Instant.now());
                                repository.save(existing);
                                log.info("[Analyzer] Updated weight for userId={}, eventId={}, newWeight={}", userId, eventId, weight);
                            } else {
                                log.debug("[Analyzer] Skipped update: newWeight={} <= oldWeight={}", weight, existing.getWeight());
                            }
                        },
                        () -> {
                            UserEventWeight newRecord = UserEventWeight.builder()
                                    .userId(userId)
                                    .eventId(eventId)
                                    .weight(weight)
                                    .timestamp(Instant.now())
                                    .build();
                            repository.save(newRecord);
                            log.info("[Analyzer] Inserted new user-event weight: userId={}, eventId={}, weight={}", userId, eventId, weight);
                        }
                );
    }

    private double mapActionToWeight(UserActionAvro action) {
        return switch (action.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}
