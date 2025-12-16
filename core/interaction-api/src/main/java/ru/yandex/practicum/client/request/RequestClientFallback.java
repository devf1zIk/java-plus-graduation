package ru.yandex.practicum.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequestClientFallback implements RequestOperations{

    @Override
    public Long getConfirmedRequestsCount(Long eventId) {
        log.warn("Request service is unavailable. Fallback: returning 0 for event ID: {}", eventId);
        return 0L;
    }
}
