package ru.yandex.practicum.client.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.enums.RequestStatus;

public interface RequestOperations {

    @GetMapping("/admin/requests/count/{eventId}")
    Long getConfirmedRequestsCount(@PathVariable @NotNull Long eventId,
                                   @RequestParam(name = "status") RequestStatus status);
}
