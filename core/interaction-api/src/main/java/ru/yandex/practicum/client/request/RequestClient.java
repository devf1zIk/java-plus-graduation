package ru.yandex.practicum.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.request.RequestDto;
import java.util.List;

@FeignClient(name = "request-service", path = "/users", fallback = RequestClientFallback.class)
public interface RequestClient extends RequestOperations {

    @Override
    @PostMapping("/{userId}/requests")
    RequestDto createRequest(@PathVariable("userId") Long userId,
                      @RequestParam("eventId") Long eventId);

    @GetMapping("/{userId}/requests")
    List<RequestDto> getRequestsByUser(@PathVariable("userId") Long userId);

    @Override
    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    RequestDto cancelByUser(@PathVariable("userId") Long userId,
                             @PathVariable("requestId") Long requestId);
}
