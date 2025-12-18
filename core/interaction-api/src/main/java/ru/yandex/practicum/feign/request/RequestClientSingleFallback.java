package ru.yandex.practicum.feign.request;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.enums.RequestStatus;

@Component
public class RequestClientSingleFallback implements RequestClientSingle {

    @Override
    public Long getCountByStatus(Long eventId, RequestStatus status) {
        return 0L;
    }
}
