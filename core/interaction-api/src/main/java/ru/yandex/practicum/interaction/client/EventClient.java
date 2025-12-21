package ru.yandex.practicum.interaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.interaction.config.FeignConfig;
import ru.yandex.practicum.interaction.dto.EventFullDto;
import ru.yandex.practicum.interaction.fallback.EventFallback;

@FeignClient(name = "event-service",
        path = "/internal/events",
        configuration = FeignConfig.class,
        fallback = EventFallback.class)
public interface EventClient {

    @GetMapping("/{id}")
    EventFullDto getEventForInternalUse(@PathVariable Long id);
}
