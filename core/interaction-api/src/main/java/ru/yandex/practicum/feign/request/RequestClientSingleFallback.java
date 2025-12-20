package ru.yandex.practicum.feign.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.enums.RequestStatus;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RequestClientSingleFallback implements RequestClientSingle {

    @Override
    public Long getCountByStatus(Long eventId, RequestStatus status) {
        return 0L;
    }

    @Override
    public boolean hasVisitedEvent(long userId, long eventId) {
        log.warn("Сервис запросов недоступен");
        return false;
    }

    @Override
    public Map<Long, Long> countConfirmedByEventIds(List<Long> eventIds) {
        log.warn("Сервис запросов недоступен");
        return null;
    }
}
