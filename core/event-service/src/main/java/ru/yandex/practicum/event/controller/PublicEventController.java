package ru.yandex.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.event.service.EventService;
import ru.yandex.practicum.interaction.dto.EventFullDto;
import ru.yandex.practicum.interaction.dto.EventShortDto;
import ru.yandex.practicum.interaction.dto.params.EventParamsPublic;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEvents(@ModelAttribute EventParamsPublic params,
                                                         HttpServletRequest request) {
        log.info("PublicEventController - Get public events. params: {}", params);
        return ResponseEntity.ok(eventService.getPublicEvents(params, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getEventByIdAndLogHit(@PathVariable Long id,
                                                              HttpServletRequest request) {
        log.info("PublicEventController - Get public event. id: {}", id);
        return ResponseEntity.ok(eventService.getEventByIdAndLogHit(id, request));
    }
}