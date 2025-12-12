package ru.yandex.practicum.client.request;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.request.RequestDto;
import java.util.List;
import java.util.Map;

public interface RequestOperations {

    @GetMapping("/admin/requests/count/{eventId}")
    Map<Long, Long> getConfirmedRequestsCount(@PathVariable List<Long> eventIds);

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<RequestDto> getEventRequests(@PathVariable Long userId,
                                      @PathVariable Long eventId);
}