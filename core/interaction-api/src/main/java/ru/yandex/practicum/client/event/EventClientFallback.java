package ru.yandex.practicum.client.event;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.event.EventDto;

import java.util.List;

@Slf4j
@Component
public class EventClientFallback implements EventOperations{

    @Override
    public EventDto getEventById(Long eventId) {
        log.error("Event service unavailable → getEventById, eventId={}", eventId);
        return null;
    }

    @Override
    public EventDto getPublicEventById(Long eventId) {
        log.error("Event service unavailable → getPublicEventById, eventId={}", eventId);
        return null;
    }

    @Override
    public EventDto getEventFullById(long eventId) {
        log.error("Event service unavailable → getEventFullById, eventId={}", eventId);
        return null;
    }

    @Override
    public EventDto getEventByIdAndInitiatorId(long userId, long eventId) {
        log.error(
                "Event service unavailable → getEventByIdAndInitiatorId, userId={}, eventId={}",
                userId, eventId
        );
        return null;
    }

    @Override
    public List<EventDto> getAllEventByInitiatorId(long userId) {
        log.error("Event service unavailable → getAllEventByInitiatorId, userId={}", userId);
        return List.of();
    }
}
