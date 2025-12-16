package ru.yandex.practicum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.service.RequestService;
import java.util.List;

@RestController
@RequestMapping(path = "/users")
public class PrivateRequestController {

    private final RequestService requestService;

    @Autowired
    public PrivateRequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    //requests
    @GetMapping("/{userId}/requests")
    public List<ParticipationRequestDto> getUserEvents(@PathVariable Long userId) {

        return requestService.getByUserId(userId);
    }

    @PostMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId, @RequestParam Long eventId) {
        return requestService.create(userId, eventId);
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelByUser(@PathVariable Long userId, @PathVariable Long requestId) {
        return requestService.cancelRequestByUser(userId, requestId);
    }
}