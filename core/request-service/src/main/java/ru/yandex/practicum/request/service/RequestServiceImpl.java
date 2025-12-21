package ru.yandex.practicum.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.interaction.client.EventClient;
import ru.yandex.practicum.interaction.client.UserClient;
import ru.yandex.practicum.interaction.dto.EventFullDto;
import ru.yandex.practicum.interaction.dto.ParticipationRequestDto;
import ru.yandex.practicum.interaction.enums.EventState;
import ru.yandex.practicum.interaction.enums.ParticipationRequestStatus;
import ru.yandex.practicum.interaction.exception.ConflictException;
import ru.yandex.practicum.interaction.exception.NotFoundException;
import ru.yandex.practicum.request.mapper.ParticipationRequestMapper;
import ru.yandex.practicum.request.model.ParticipationRequest;
import ru.yandex.practicum.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;


    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userClient.getUserById(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long requesterId, Long eventId) {
        userClient.getUserById(requesterId);
        EventFullDto event = eventClient.getEventForInternalUse(eventId);

        if (requestRepository.existsByRequesterIdAndEventId(requesterId, eventId)) {
            throw new ConflictException("User already sent a request for this event.");
        }
        if (requesterId.equals(event.getInitiator().getId())) {
            throw new ConflictException("The event initiator cannot submit a participation request for their own event.");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Participation in an unpublished event is not allowed.");
        }

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);

        if (confirmedRequests >= event.getParticipantLimit() && event.getParticipantLimit() != 0) {
            throw new ConflictException("The event has reached the participation request limit.");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requesterId(requesterId)
                .eventId(event.getId())
                .created(LocalDateTime.now())
                .status(ParticipationRequestStatus.PENDING)
                .build();

        if (!event.isRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationRequestStatus.CONFIRMED);
        }

        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        userClient.getUserById(userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("User can cancel only their own requests.");
        }

        request.setStatus(ParticipationRequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventId(Long eventId) {
        return requestRepository.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventIdInAndStatus(List<Long> eventIds,
                                                                     ParticipationRequestStatus status) {
        return ParticipationRequestMapper
                .toDtoList(requestRepository.findAllByEventIdInAndStatus(eventIds, status));
    }

    @Override
    public Long countByEventIdAndStatus(Long eventId, ParticipationRequestStatus status) {
        return requestRepository.countByEventIdAndStatus(eventId, status);
    }

    @Override
    @Transactional
    public Integer updateAllRequests(List<ParticipationRequestDto> updatedRequests) {

        List<ParticipationRequest> requestsToSave = ParticipationRequestMapper.toEntityList(updatedRequests);
        List<ParticipationRequest> savedRequests = requestRepository.saveAll(requestsToSave);

        return savedRequests.size();
    }
}