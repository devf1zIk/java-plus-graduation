package ru.yandex.practicum.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.ParticipationRequest;
import ru.yandex.practicum.service.RequestService;
import java.util.List;

@RestController
@RequestMapping("/admin/requests")
@RequiredArgsConstructor
public class AdminRequestController {

    private final RequestService requestService;

    @GetMapping("/count/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequest> getConfirmedRequestsCount(@PathVariable @NotNull Long eventId) {
        return requestService.getConfirmedRequestsCount(eventId);
    }
}
