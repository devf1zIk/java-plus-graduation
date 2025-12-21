package ru.yandex.practicum.interaction.fallback;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.interaction.client.ParticipationRequestClient;
import ru.yandex.practicum.interaction.dto.ParticipationRequestDto;
import ru.yandex.practicum.interaction.enums.ParticipationRequestStatus;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ParticipationRequestFallback implements ParticipationRequestClient {

    @Override
    public List<ParticipationRequestDto> getAllByEventId(Long eventId) {
        log.warn("[ParticipationRequestClient#getAllByEventId] " +
                "Fallback triggered: get all by event ID {} failed.", eventId);
        return Collections.emptyList();
    }

    @Override
    public List<ParticipationRequestDto> getAllByEventIdsAndStatus(List<Long> eventIds, ParticipationRequestStatus status) {
        log.warn("[ParticipationRequestClient#getAllByEventIdsAndStatus] " +
                "Fallback triggered: get all by event IDs and status {} failed. Event IDs: {}", status, eventIds);
        return Collections.emptyList();
    }

    @Override
    public long countByEventIdAndStatus(Long eventId, ParticipationRequestStatus status) {
        log.warn("[ParticipationRequestClient#countByEventIdAndStatus] " +
                "Fallback triggered: count by event ID {} and status {} failed.", eventId, status);
        return -1L;
    }

    @Override
    public Integer updateAllRequests(List<ParticipationRequestDto> updatedRequests) {
        log.warn("[ParticipationRequestClient#updateAllRequests] " +
                "Fallback triggered: update all requests failed. No action performed.");
        return -1;
    }
}