package ru.yandex.practicum.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.event.*;

@Slf4j
@Component
public class EventClientFallback implements EventOperations{

    @Override
    public EventDto getPublicEvent(Long eventId) {
        log.warn("Event service unavailable → getPublicEvent({})", eventId);
        return null;
    }
}
