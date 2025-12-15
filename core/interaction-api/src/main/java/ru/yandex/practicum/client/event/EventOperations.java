package ru.yandex.practicum.client.event;

import org.springframework.web.bind.annotation.GetMapping;
import ru.yandex.practicum.dto.event.*;

public interface EventOperations {

    @GetMapping("/events/{id}")
    EventFullDto getPublicEvent(Long eventId);
}
