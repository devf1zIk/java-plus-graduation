package ru.yandex.practicum.main.event.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.main.event.service.EventService;

@RestController
@RequestMapping("/internal/events")
@Validated
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventDto getEvent(@PathVariable("eventId") @Positive Long eventId) {
        return eventService.getById(eventId);
    }
}
