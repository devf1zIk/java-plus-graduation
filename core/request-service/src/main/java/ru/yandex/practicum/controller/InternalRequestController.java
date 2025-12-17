package ru.yandex.practicum.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.service.RequestService;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {


    private final RequestService requestService;

    @GetMapping("/event/{eventId}/count/{status}")
    public Long countByStatus(@PathVariable @Positive Long eventId, @PathVariable @NotNull RequestStatus status) {
        return requestService.countByEventIdAndStatus(eventId, status);
    }
}
