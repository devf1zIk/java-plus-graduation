package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.service.RequestService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/requests")
@RequiredArgsConstructor
public class RequestAdminController {

    private final RequestService requestService;

    @GetMapping("/count")
    public Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds, @RequestParam RequestStatus status) {
        return requestService.getConfirmedRequestsCountForEvents(eventIds, status);
    }
}
