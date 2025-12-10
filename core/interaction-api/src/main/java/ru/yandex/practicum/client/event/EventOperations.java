package ru.yandex.practicum.client.event;

import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResponse;
import java.util.List;

public interface EventOperations {

    EventDto getPublicEvent(Long eventId);

    List<EventShortDto> getPublicEvents(String text,
                                        List<Long> categories,
                                        Boolean paid,
                                        String rangeStart,
                                        String rangeEnd,
                                        boolean onlyAvailable,
                                        String sort,
                                        int from,
                                        int size);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    EventDto createEvent(Long userId, CreateNewEventDto dto);

    EventDto getUserEvent(Long userId, Long eventId);

    EventDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest dto);

    List<RequestDto> getEventRequests(Long userId, Long eventId);

    RequestStatusUpdateResponse updateEventRequests(Long userId, Long eventId, RequestStatusUpdateRequest dto);

    List<EventDto> getAdminEvents(List<Long> users,
                                  List<String> states,
                                  List<Long> categories,
                                  String rangeStart,
                                  String rangeEnd,
                                  int from,
                                  int size);

    EventDto updateEventByAdmin(Long eventId, UpdateEventAdminDto dto);
}
