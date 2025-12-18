package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.event.EventShortForRequestDto;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResponse;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.ForbiddenException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.feign.event.EventClient;
import ru.yandex.practicum.feign.request.RequestClient;
import ru.yandex.practicum.feign.request.RequestClientSingle;
import ru.yandex.practicum.feign.user.UserClient;
import ru.yandex.practicum.mapper.RequestMapper;
import ru.yandex.practicum.model.ParticipationRequest;
import ru.yandex.practicum.repository.RequestRepository;
import java.time.LocalDateTime;
import java.util.*;
import static ru.yandex.practicum.enums.RequestStatus.CONFIRMED;
import static ru.yandex.practicum.enums.RequestStatus.REJECTED;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestClient requestClient;
    private final RequestClientSingle requestClientSingle;

    public List<RequestDto> getEventRequests(Long userId, Long eventId) {
        UserShortDto user;
        try {
            user = userClient.getById(userId);
        } catch (Exception e) {
            log.error("Ошибка при получении пользователя с id {}: {}", userId, e.getMessage());
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        EventShortForRequestDto event;
        try {
            event = eventClient.getById(eventId);
        } catch (Exception e) {
            log.error("Ошибка при получении события с id {}: {}", eventId, e.getMessage());
            throw new NotFoundException("Событие с id " + eventId + " не найдено");
        }

        if (!Objects.equals(event.getOwnerId(), userId)) {
            throw new ForbiddenException("User с id " + userId + " не владелец события " + eventId);
        }
        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);
        return requests.stream()
                .map(RequestMapper::fromRequestToRequestDto)
                .toList();
    }

    public RequestStatusUpdateResponse updateRequest(Long userId, Long eventId, RequestStatusUpdateRequest requestDto) {
        try {
            userClient.getById(userId);
        } catch (Exception e) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        EventShortForRequestDto event;
        try {
            event = eventClient.getById(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Событие с id " + eventId + " не найдено");
        }

        if (!Objects.equals(event.getOwnerId(), userId)) {
            throw new ForbiddenException("User с id " + userId + " не владелец события " + eventId);
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(requestDto.getRequestIds());
        if (requests.isEmpty() || requests.stream().noneMatch(r -> r.getStatus() == RequestStatus.PENDING)) {
            throw new ConflictException("Нет pending-запросов для обновления");
        }

        Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        RequestStatusUpdateResponse response = new RequestStatusUpdateResponse(new HashSet<>(), new HashSet<>());

        if (!event.getIsModerated() || event.getParticipantLimit() == 0) {
            requests.forEach(r -> r.setStatus(RequestStatus.CONFIRMED));
            response.getConfirmedRequests().addAll(requests.stream().map(RequestMapper::fromRequestToRequestDto).toList());
        } else if (requestDto.getStatus() == CONFIRMED) {
            if (confirmedCount >= event.getParticipantLimit()) {
                throw new ConflictException("Лимит участников достигнут");
            }

            for (ParticipationRequest request : requests) {
                if (request.getStatus() == RequestStatus.PENDING) {
                    if (confirmedCount < event.getParticipantLimit()) {
                        request.setStatus(RequestStatus.CONFIRMED);
                        response.getConfirmedRequests().add(RequestMapper.fromRequestToRequestDto(request));
                        confirmedCount++;
                    } else {
                        request.setStatus(RequestStatus.REJECTED);
                        response.getRejectedRequests().add(RequestMapper.fromRequestToRequestDto(request));
                    }
                }
            }
        } else if (requestDto.getStatus() == REJECTED) {
            for (ParticipationRequest request : requests) {
                if (request.getStatus() == RequestStatus.CONFIRMED) {
                    throw new ConflictException("Нельзя отклонить уже подтвержденную заявку");
                }
                if (request.getStatus() == RequestStatus.PENDING) {
                    request.setStatus(RequestStatus.REJECTED);
                    response.getRejectedRequests().add(RequestMapper.fromRequestToRequestDto(request));
                }
            }
        }

        requestRepository.saveAll(requests);
        return response;
    }

    public List<RequestDto> getByUserId(Long userId) {
        try {
            userClient.getById(userId);
        } catch (Exception e) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::fromRequestToRequestDto)
                .toList();
    }

    public RequestDto create(Long userId, Long eventId) {
        UserShortDto user;
        EventShortForRequestDto event;
        try {
            user = userClient.getById(userId);
            event = eventClient.getById(eventId);
        } catch (Exception e) {
            log.error("Ошибка Feign клиента при создании заявки: {}", e.getMessage());
            throw new NotFoundException("Не удалось найти пользователя или событие");
        }

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
        return RequestMapper.fromRequestToRequestDto(saved);
    }

    public RequestDto cancelRequestByUser(Long userId, Long requestId) {
        try {
            userClient.getById(userId);
        } catch (Exception e) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new ConflictException("Можно отменить только свою заявку");
        }

        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.fromRequestToRequestDto(requestRepository.save(request));
    }

    public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> map = requestClient.getConfirmedCounts(eventIds);
        return map != null ? map : Collections.emptyMap();
    }

    public Long getCountByStatus(Long eventId, RequestStatus status) {
        try {
            return requestClientSingle.getCountByStatus(eventId, status);
        } catch (Exception e) {
            log.error("Ошибка при получении количества заявок для события {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }
}