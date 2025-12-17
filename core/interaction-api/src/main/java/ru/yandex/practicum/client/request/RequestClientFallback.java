package ru.yandex.practicum.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.exception.model.ServiceUnavailableException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RequestClientFallback implements RequestOperations{

    @Override
    public Long countByStatus(Long eventId, RequestStatus status) {
        log.error("Request service unavailable → countByStatus, eventId={}, status={}", eventId, status);
        throw new ServiceUnavailableException("Request service is unavailable");
    }

    @Override
    public Map<Long, Long> countByStatusBatch(List<Long> eventIds, RequestStatus status) {
        log.error("Request service unavailable → countByStatusBatch, eventIds={}, status={}", eventIds, status);
        throw new ServiceUnavailableException("Request service is unavailable");
    }
}
