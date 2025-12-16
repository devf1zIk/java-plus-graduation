package ru.yandex.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResponse;
import ru.yandex.practicum.service.RequestService;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/events/{eventId}/requests")
public class PrivateEventRequestController {

    private final RequestService requestService;


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId, @PathVariable @Positive Long eventId) {
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping
    @ResponseStatus(HttpStatus.OK)
    public RequestStatusUpdateResponse updateRequest(@PathVariable @Positive Long userId, @PathVariable @Positive Long eventId,
                                                     @RequestBody RequestStatusUpdateRequest request) {
        return requestService.updateRequest(userId, eventId, request);
    }
}
