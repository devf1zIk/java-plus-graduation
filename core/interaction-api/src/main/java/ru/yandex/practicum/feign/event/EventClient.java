package ru.yandex.practicum.feign.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.event.EventShortForRequestDto;

@FeignClient(name = "EVENT-SERVICE", path = "/internal/events")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventShortForRequestDto getById(@PathVariable Long eventId);
}
