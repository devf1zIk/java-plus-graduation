package ru.yandex.practicum.event.controller.pub;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.grpc.stats.messages.RecommendedEventProto;
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
                                                              @RequestHeader("X-EWM-USER-ID") long userId) {
        log.info("PublicEventController - Get public event. id: {}", id);
        return ResponseEntity.ok(eventService.getEventByIdAndLogHit(id, userId));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendedEventProto>> getRecommendationsForUser(
            @RequestHeader("X-EWM-USER-ID") long userId,
            @RequestParam(defaultValue = "10") int maxResults) {
        log.info("PublicEventController - Get recommendations for user. id: {}, maxResults: {}", userId, maxResults);
        return ResponseEntity.ok(eventService.getRecommendationsForUser(userId, maxResults));
    }


    @PutMapping("/{eventId}/like")
    public ResponseEntity<Void> sendLikeToCollector(@RequestHeader("X-EWM-USER-ID") long userId,
                                                    @PathVariable long eventId) {
        log.info("PublicEventController - Send like to Collector. userId: {}, eventId: {}", userId, eventId);
        eventService.sendLikeToCollector(userId, eventId);
        return ResponseEntity.noContent().build();
    }
}