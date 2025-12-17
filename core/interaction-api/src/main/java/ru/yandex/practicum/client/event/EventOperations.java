package ru.yandex.practicum.client.event;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.event.EventDto;

public interface EventOperations {

    @GetMapping("/{eventId}")
    EventDto getEvent(@PathVariable @NotNull Long eventId);
}
