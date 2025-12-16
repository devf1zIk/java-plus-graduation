package ru.yandex.practicum.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.event.EventDto;

@Slf4j
@Component
public class EventClientFallback implements EventOperations{

    @Override
    public EventDto getEvent(Long eventId) {
        log.error("Event service unavailable → getEvent, eventId={}", eventId);
        return null;
    }
}
