package ru.yandex.practicum.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import ru.yandex.practicum.config.FeignRetryConfig;

@FeignClient(name = "event-service", configuration = FeignRetryConfig.class,fallback = EventClientFallback.class)
public interface EventClient extends EventOperations {
}
