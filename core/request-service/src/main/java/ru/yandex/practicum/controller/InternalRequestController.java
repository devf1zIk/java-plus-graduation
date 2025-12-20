package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.service.RequestService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final RequestService requestService;

    @GetMapping("/confirmed-counts")
    public Map<Long, Long> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds) {
        return requestService.getConfirmedRequestsCount(eventIds);
    }

    @GetMapping("/counts")
    public Long getCountByStatus(@RequestParam Long eventId,
                                 @RequestParam(defaultValue = "CONFIRMED") RequestStatus status) {
        return requestService.getCountByStatus(eventId, status);
    }

    @GetMapping("/confirmed")
    public boolean hasConfirmedRequest(@RequestParam Long userId, @RequestParam Long eventId) {
        return requestService.hasVisitedEvent(userId, eventId);
    }

    @PostMapping("/confirmed/batch")
    public Map<Long, Long> countConfirmedByEventIds(@RequestBody List<Long> eventIds) {
        return requestService.countConfirmedByEventIds(eventIds);
    }
}
