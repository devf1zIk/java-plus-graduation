package ru.yandex.practicum.feign.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.enums.RequestStatus;

@FeignClient(
        name = "REQUEST-SERVICE",
        path = "/internal/requests",
        fallback = RequestClientSingleFallback.class
)
public interface RequestClientSingle {
    @GetMapping("/counts")
    Long getCountByStatus(@RequestParam("eventId") Long eventId,
                          @RequestParam(value = "status", defaultValue = "CONFIRMED") RequestStatus status);
}
