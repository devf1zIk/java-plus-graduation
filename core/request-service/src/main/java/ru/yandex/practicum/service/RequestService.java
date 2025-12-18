package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.event.EventShortForRequestDto;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResponse;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.feign.event.EventClient;
import ru.yandex.practicum.feign.user.UserClient;
import ru.yandex.practicum.mapper.RequestMapper;
import ru.yandex.practicum.model.ParticipationRequest;
import ru.yandex.practicum.repository.RequestRepository;
import java.time.LocalDateTime;
import java.util.*;
import static ru.yandex.practicum.enums.RequestStatus.CONFIRMED;
import static ru.yandex.practicum.enums.RequestStatus.REJECTED;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    public List<RequestDto> getEventRequests(Long userId, Long eventId) {
        userClient.getById(userId);
        EventShortForRequestDto event = eventClient.getById(eventId);
        if (!Objects.equals(event.getOwnerId(), userId)) {
            throw new NotFoundException("User с id " + userId + " не владелец события " + eventId);
        }
        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);
        return requests.stream()
                .map(RequestMapper::fromRequestTpRequestDto)
                .toList();
    }

    public RequestStatusUpdateResponse updateRequest(Long userId, Long eventId, RequestStatusUpdateRequest requestDto) {
        userClient.getById(userId);
        EventShortForRequestDto event = eventClient.getById(eventId);

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(requestDto.getRequestIds());
        if (requests.isEmpty() || requests.stream().noneMatch(r -> r.getStatus() == RequestStatus.PENDING)) {
            throw new ConflictException("Нет pending-запросов для обновления");
        }

        Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        RequestStatusUpdateResponse response = new RequestStatusUpdateResponse(new HashSet<>(), new HashSet<>());

        if (!event.getIsModerated() || event.getParticipantLimit() == 0) {
            requests.forEach(r -> r.setStatus(RequestStatus.CONFIRMED));
            response.getConfirmedRequests().addAll(requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
        } else if (confirmedCount + requests.size() > event.getParticipantLimit()) {
            throw new ConflictException("Лимит участников превышен");
        } else if (requestDto.getStatus() == CONFIRMED) {
            requests.forEach(r -> r.setStatus(RequestStatus.CONFIRMED));
            response.getConfirmedRequests().addAll(requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
        } else if (requestDto.getStatus() == REJECTED) {
            requests.forEach(r -> r.setStatus(RequestStatus.REJECTED));
            response.getRejectedRequests().addAll(requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
        }

        requestRepository.saveAll(requests);
        return response;
    }

    public List<RequestDto> getByUserId(Long userId) {
        userClient.getById(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::fromRequestTpRequestDto)
                .toList();
    }

    public RequestDto create(Long userId, Long eventId) {
        UserShortDto user = userClient.getById(userId);
        EventShortForRequestDto event = eventClient.getById(eventId);

        if (Objects.equals(user.getId(), event.getOwnerId())) {
            throw new ConflictException("Инициатор не может подать заявку на своё событие");
        }
        if (event.getPublishedOn() == null) {
            throw new ConflictException("Событие не опубликовано");
        }

        Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (confirmedCount >= event.getParticipantLimit() && event.getParticipantLimit() > 0) {
            throw new ConflictException("Лимит участников достигнут");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Заявка уже существует");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .createdOn(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(event.getParticipantLimit() == 0 || !event.getIsModerated() ? RequestStatus.CONFIRMED : RequestStatus.PENDING)
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        return RequestMapper.fromRequestTpRequestDto(saved);
    }

    public RequestDto cancelRequestByUser(Long userId, Long requestId) {
        userClient.getById(userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new ConflictException("Можно отменить только свою заявку");
        }

        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.fromRequestTpRequestDto(requestRepository.save(request));
    }

    public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> raw = requestRepository.countConfirmedByEventIdsRaw(eventIds);
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : raw) {
            result.put((Long) row[0], (Long) row[1]);
        }
        eventIds.forEach(id -> result.putIfAbsent(id, 0L));
        return result;
    }

    public Long getCountByStatus(Long eventId, RequestStatus status) {
        return requestRepository.countByEventIdAndStatus(eventId, status);
    }
}