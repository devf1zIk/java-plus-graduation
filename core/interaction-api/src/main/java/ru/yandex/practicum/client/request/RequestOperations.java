package ru.yandex.practicum.client.request;

import ru.yandex.practicum.dto.request.RequestDto;
import java.util.List;

public interface RequestOperations {

    RequestDto createRequest(Long userId, Long eventId);

    List<RequestDto> getUserEvents(Long userId);

    RequestDto cancelByUser(Long userId, Long requestId);
}
