package ru.yandex.practicum.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.enums.RequestStatus;

@Slf4j
@Component
public class RequestClientFallback implements RequestOperations{

    @Override
    public Long countByStatus(Long eventId, RequestStatus status) {
        log.warn("Request service is unavailable. Fallback: returning 0 for event ID: {}, status: {}", eventId, status);
        return 0L;
    }
}
