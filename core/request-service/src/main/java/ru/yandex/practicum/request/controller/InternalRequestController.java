package ru.yandex.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.interaction.dto.ParticipationRequestDto;
import ru.yandex.practicum.interaction.enums.ParticipationRequestStatus;
import ru.yandex.practicum.request.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events/requests")
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/byEventId")
    public ResponseEntity<List<ParticipationRequestDto>> getAllByEventId(@RequestParam Long eventId) {
        log.info("Getting all requests by eventId: {}", eventId);
        return ResponseEntity.ok(requestService.findAllByEventId(eventId));
    }

    @GetMapping("/byEventIdsAndStatus")
    public ResponseEntity<List<ParticipationRequestDto>> getAllByEventIdsAndStatus(
            @RequestParam List<Long> eventIds,
            @RequestParam ParticipationRequestStatus status) {
        log.info("Getting all requests by eventIds: {} and Status: {}", eventIds, status);
        return ResponseEntity.ok(requestService.findAllByEventIdInAndStatus(eventIds, status));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countByEventIdAndStatus(@RequestParam Long eventId,
                                                        @RequestParam ParticipationRequestStatus status) {
        log.info("Counting all requests by eventId: {} with status: {}", eventId, status);
        return ResponseEntity.ok(requestService.countByEventIdAndStatus(eventId, status));
    }

    @PutMapping
    public ResponseEntity<Integer> updateAllRequests(@RequestBody List<ParticipationRequestDto> updatedRequests) {
        log.info("Updating all requests:{}", updatedRequests);
        return ResponseEntity.ok(requestService.updateAllRequests(updatedRequests));
    }
}
