package ru.yandex.practicum.client.request;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface RequestOperations {

    @GetMapping("/admin/requests/count/{eventId}")
    Long getConfirmedRequestsCount(@PathVariable Long eventId);
}
