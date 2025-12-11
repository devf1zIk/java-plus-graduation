package ru.yandex.practicum.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResponse;
import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", fallback = RequestClientFallback.class)
public interface RequestClient extends RequestOperations {

    @PostMapping("/{userId}/requests")
    RequestDto createRequest(@PathVariable("userId") Long userId,
                      @RequestParam("eventId") Long eventId);

    @GetMapping("/{userId}/requests")
    List<RequestDto> getRequestsByUser(@PathVariable("userId") Long userId);

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    RequestDto cancelByUser(@PathVariable("userId") Long userId,
                             @PathVariable("requestId") Long requestId);

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<RequestDto> getEventRequests(@PathVariable("userId") Long userId,
                                      @PathVariable("eventId") Long eventId);

    @Override
    @GetMapping("/admin/requests/count/{eventId}")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam("ids") List<Long> eventIds);


    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    RequestStatusUpdateResponse updateEventRequests(@PathVariable("userId") Long userId,
                                                    @PathVariable("eventId") Long eventId,
                                                    @RequestBody RequestStatusUpdateRequest dto);
}
