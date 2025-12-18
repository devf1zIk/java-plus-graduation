package ru.yandex.practicum.feign.event;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.event.EventShortForRequestDto;

@Component
public class EventClientFallback implements EventClient {

    @Override
    public EventShortForRequestDto getById(Long eventId) {
        return EventShortForRequestDto.builder()
                .id(eventId)
                .ownerId(-1L)
                .publishedOn(null)
                .isModerated(true)
                .participantLimit(0)
                .build();
    }
}
