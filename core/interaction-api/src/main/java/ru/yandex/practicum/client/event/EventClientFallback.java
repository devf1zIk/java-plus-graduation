package ru.yandex.practicum.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResponse;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class EventClientFallback implements EventOperations{

    @Override
    public EventDto getPublicEvent(Long eventId) {
        log.warn("Event service unavailable → getPublicEvent({})", eventId);
        return null;
    }

    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, boolean onlyAvailable,
                                               String sort, int from, int size) {
        log.warn("Event service unavailable → getPublicEvents");
        return Collections.emptyList();
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        log.warn("Event service unavailable → getUserEvents(userId={})", userId);
        return Collections.emptyList();
    }

    @Override
    public EventDto createEvent(Long userId, CreateNewEventDto dto) {
        log.warn("Event service unavailable → createEvent(userId={})", userId);
        return null;
    }

    @Override
    public EventDto getUserEvent(Long userId, Long eventId) {
        log.warn("Event service unavailable → getUserEvent(userId={}, eventId={})", userId, eventId);
        return null;
    }

    @Override
    public EventDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest dto) {
        log.warn("Event service unavailable → updateEventByUser(userId={}, eventId={})", userId, eventId);
        return null;
    }

    @Override
    public List<RequestDto> getEventRequests(Long userId, Long eventId) {
        log.warn("Event service unavailable → getEventRequests(userId={}, eventId={})", userId, eventId);
        return Collections.emptyList();
    }

    @Override
    public RequestStatusUpdateResponse updateEventRequests(Long userId, Long eventId, RequestStatusUpdateRequest dto) {
        log.warn("Event service unavailable → updateEventRequests(userId={}, eventId={})", userId, eventId);
        return null;
    }

    @Override
    public List<EventDto> getAdminEvents(List<Long> users, List<String> states, List<Long> categories,
                                         String rangeStart, String rangeEnd, int from, int size) {
        log.warn("Event service unavailable → getAdminEvents");
        return Collections.emptyList();
    }

    @Override
    public EventDto updateEventByAdmin(Long eventId, UpdateEventAdminDto dto) {
        log.warn("Event service unavailable → updateEventByAdmin(eventId={})", eventId);
        return null;
    }
}
