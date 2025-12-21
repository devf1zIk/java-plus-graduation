package ru.yandex.practicum.event.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.event.service.EventService;
import ru.yandex.practicum.interaction.dto.EventFullDto;

@Slf4j
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getEventForInternalUse(@PathVariable Long id) {
        log.info("PublicEventController - Get public event or throw exception. id: {}", id);
        return ResponseEntity.ok(eventService.getEventForInternalUse(id));
    }

}
