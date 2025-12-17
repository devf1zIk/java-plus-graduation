package ru.yandex.practicum.client.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.enums.RequestStatus;

public interface RequestOperations {

    @GetMapping("/event/{eventId}/count/{status}")
    Long countByStatus(@PathVariable @Positive Long eventId, @PathVariable @NotNull RequestStatus status);
}
