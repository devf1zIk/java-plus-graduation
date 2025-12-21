package ru.yandex.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.yandex.practicum.interaction.dto.*;
import ru.yandex.practicum.interaction.dto.params.EventParamsAdmin;
import ru.yandex.practicum.interaction.dto.params.EventParamsPublic;
import ru.yandex.practicum.interaction.dto.params.UserParamsAdmin;

import java.util.List;

public interface EventService {

    List<EventShortDto> getUserEvents(Long userId, UserParamsAdmin params);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto createUserEvent(Long userId, NewEventDto dto);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto);

    List<EventShortDto> getPublicEvents(EventParamsPublic params, HttpServletRequest request);

    EventFullDto getEventByIdAndLogHit(Long eventId, HttpServletRequest request);

    EventFullDto getEventForInternalUse(Long eventId);

    List<EventFullDto> getEventsByAdmin(EventParamsAdmin params);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest requestUpdate);

    List<ParticipationRequestDto> getAllParticipationRequestsByUserIdAndEventId(Long userId, Long eventId);
}