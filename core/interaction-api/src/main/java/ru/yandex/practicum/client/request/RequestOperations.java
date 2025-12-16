package ru.yandex.practicum.client.request;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.enums.RequestStatus;

public interface RequestOperations {

    @GetMapping("/event/{eventId}/count/{status}")
    Long countByStatus(@PathVariable Long eventId, @PathVariable RequestStatus status);
}
