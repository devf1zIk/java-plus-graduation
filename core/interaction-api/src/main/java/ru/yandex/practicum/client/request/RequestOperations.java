package ru.yandex.practicum.client.request;

import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.Map;

public interface RequestOperations {

    @GetMapping("/admin/requests/count/{eventId}")
    Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds);
}
