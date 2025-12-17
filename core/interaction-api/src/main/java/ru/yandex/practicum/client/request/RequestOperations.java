package ru.yandex.practicum.client.request;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.enums.RequestStatus;
import java.util.List;
import java.util.Map;

public interface RequestOperations {

    @GetMapping("/event/{eventId}/count/{status}")
    Long countByStatus(@PathVariable @Positive Long eventId, @PathVariable RequestStatus status);

    @PostMapping("/events/count/{status}")
    Map<Long, Long> countByStatusBatch(@RequestBody List<Long> eventIds, @PathVariable RequestStatus status);
}
