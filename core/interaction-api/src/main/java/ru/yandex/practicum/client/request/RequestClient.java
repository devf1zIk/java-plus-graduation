package ru.yandex.practicum.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import ru.yandex.practicum.config.FeignRetryConfig;

@FeignClient(name = "request-service", configuration = FeignRetryConfig.class, fallback = RequestClientFallback.class)
public interface RequestClient extends RequestOperations {
}
