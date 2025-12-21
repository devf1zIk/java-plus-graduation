package ru.yandex.practicum.request.service;

import ru.yandex.practicum.interaction.dto.ParticipationRequestDto;
import ru.yandex.practicum.interaction.enums.ParticipationRequestStatus;

import java.util.List;

public interface RequestService {
    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> findAllByEventId(Long eventId);

    List<ParticipationRequestDto> findAllByEventIdInAndStatus(List<Long> eventIds, ParticipationRequestStatus status);

    Long countByEventIdAndStatus(Long eventId, ParticipationRequestStatus status);

    Integer updateAllRequests(List<ParticipationRequestDto> updatedRequests);
}