package ru.yandex.practicum.feign.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.enums.RequestStatus;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "REQUEST-SERVICE",
        path = "/internal/requests",
        fallback = RequestClientSingleFallback.class
)
public interface RequestClientSingle {
    @GetMapping("/counts")
    Long getCountByStatus(@RequestParam("eventId") Long eventId,
                          @RequestParam(value = "status", defaultValue = "CONFIRMED") RequestStatus status);

    @GetMapping("/confirmed")
    boolean hasVisitedEvent(@RequestParam long userId, @RequestParam long eventId);

    @PostMapping("/confirmed/batch")
    Map<Long, Long> countConfirmedByEventIds(@RequestBody List<Long> eventIds);
}
