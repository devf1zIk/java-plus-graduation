package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResponse;
import ru.yandex.practicum.service.RequestService;
import java.util.List;

@RestController
@RequestMapping(path = "/users")
public class RequestPrivateController {

    private final RequestService requestService;

    @Autowired
    public RequestPrivateController(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/{userId}/requests")
    public List<RequestDto> getUserEvents(@PathVariable Long userId) {

        return requestService.getByUserId(userId);
    }

    @PostMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto createRequest(@PathVariable Long userId, @RequestParam Long eventId) {
        return requestService.create(userId, eventId);
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public RequestDto cancelByUser(@PathVariable Long userId, @PathVariable Long requestId) {
        return requestService.cancelRequestByUser(userId, requestId);
    }

    @PatchMapping("/{userId}/events/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public RequestStatusUpdateResponse updateRequest(@PathVariable Long userId, @PathVariable Long eventId,
                                                     @RequestBody @Valid RequestStatusUpdateRequest request) {
        return requestService.updateRequest(userId, eventId, request);
    }
}