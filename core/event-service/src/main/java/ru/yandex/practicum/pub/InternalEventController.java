package ru.yandex.practicum.pub;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.event.EventShortForRequestDto;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.service.EventService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventShortForRequestDto getById(@PathVariable Long eventId) {
        Event event = eventService.getEventIfExist(eventId);
        return EventShortForRequestDto.builder()
                .id(event.getId())
                .ownerId(event.getOwnerId())
                .publishedOn(event.getPublishedOn())
                .isModerated(event.getIsModerated())
                .participantLimit(event.getParticipantLimit())
                .build();
    }
}