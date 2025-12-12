package ru.yandex.practicum.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.request.RequestDto;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RequestClientFallback implements RequestOperations {

    @Override
    public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
        log.warn("Request service unavailable → getConfirmedRequestsCount: eventIds={}", eventIds);
        return Map.of();
    }

    @Override
    public List<RequestDto> getEventRequests(Long userId, Long eventId) {
        log.warn("Request service unavailable → getEventRequests: userId={},eventIds={}", userId,eventId);
        return List.of();
    }
}
