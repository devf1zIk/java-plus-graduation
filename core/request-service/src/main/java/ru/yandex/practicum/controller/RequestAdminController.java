package ru.yandex.practicum.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.service.RequestService;
import java.util.List;

@RestController
@RequestMapping("/admin/requests")
@RequiredArgsConstructor
public class RequestAdminController {

    private final RequestService requestService;

    @GetMapping("/count/{eventId}")
    public Long count(@PathVariable @NonNull Long eventId){
        return requestService.getConfirmedRequestsCountForEvents(List.of(eventId)).get(eventId);
    }
}
