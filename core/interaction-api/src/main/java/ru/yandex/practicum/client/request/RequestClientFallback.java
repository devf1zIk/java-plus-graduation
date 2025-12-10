package ru.yandex.practicum.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.request.RequestDto;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RequestClientFallback implements RequestOperations {

    @Override
    public RequestDto createRequest(Long userId, Long eventId) {
        log.warn("Request service unavailable → create: userId={}, eventId={}", userId, eventId);
        return null;
    }

    @Override
    public List<RequestDto> getUserEvents(Long userId) {
        log.warn("Request service unavailable → getRequestsByUser: userId={}", userId);
        return Collections.emptyList();
    }

    @Override
    public RequestDto cancelByUser(Long userId, Long requestId) {
        log.warn("Request service unavailable → cancelRequest: userId={}, requestId={}", userId, requestId);
        return null;
    }
}
