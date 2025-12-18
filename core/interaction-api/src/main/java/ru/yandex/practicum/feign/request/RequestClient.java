package ru.yandex.practicum.feign.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@FeignClient(
        name = "REQUEST-SERVICE",
        path = "/internal/requests",
        fallback = RequestClientFallback.class
)
public interface RequestClient {

    @GetMapping("/confirmed-counts")
    Map<Long, Long> getConfirmedCounts(@RequestParam List<Long> eventIds);
}
