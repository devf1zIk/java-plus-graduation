package ru.yandex.practicum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.request.RequestOperations;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.service.RequestService;
import java.util.List;

@RestController
@RequestMapping(path = "/users")
public class RequestController implements RequestOperations {

    private final RequestService requestService;

    @Autowired
    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @Override
    @GetMapping("/{userId}/requests")
    public List<RequestDto> getUserEvents(@PathVariable Long userId) {

        return requestService.getByUserId(userId);
    }

    @Override
    @PostMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto createRequest(@PathVariable Long userId, @RequestParam Long eventId) {
        return requestService.create(userId, eventId);
    }

    @Override
    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public RequestDto cancelByUser(@PathVariable Long userId, @PathVariable Long requestId) {
        return requestService.cancelRequestByUser(userId, requestId);
    }
}