package ru.yandex.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.event.service.EventService;
import ru.yandex.practicum.interaction.dto.EventFullDto;
import ru.yandex.practicum.interaction.dto.UpdateEventAdminRequest;
import ru.yandex.practicum.interaction.dto.params.EventParamsAdmin;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventFullDto>> getEvents(@ModelAttribute EventParamsAdmin params) {
        log.info("AdminEventController - Get events for {}", params);
        return ResponseEntity.ok(eventService.getEventsByAdmin(params));
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> update(@PathVariable Long eventId,
                                               @RequestBody @Valid UpdateEventAdminRequest dto) {
        log.info("AdminEventController - Updating event: {}", dto);
        return ResponseEntity.ok(eventService.updateEventByAdmin(eventId, dto));
    }
}