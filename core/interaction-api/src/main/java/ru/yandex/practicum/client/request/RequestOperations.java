package ru.yandex.practicum.client.request;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.enums.RequestStatus;
import java.util.List;
import java.util.Map;

public interface RequestOperations {

    @GetMapping("/admin/requests/count")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam("eventIds") List<Long> eventIds, @RequestParam("status") RequestStatus status);

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<RequestDto> getEventRequests(@PathVariable Long userId,
                                      @PathVariable Long eventId);
}