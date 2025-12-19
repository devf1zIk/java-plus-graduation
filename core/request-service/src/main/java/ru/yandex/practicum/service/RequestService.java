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
        log.info("START updateRequest for userId: {}, eventId: {}, requestDto: {}", userId, eventId, requestDto);

        if (requestDto == null || requestDto.getRequestIds() == null || requestDto.getStatus() == null) {
            log.error("Invalid request data: requestDto is null or contains null fields");
            throw new ConflictException("Некорректные данные для обновления заявки");
        }

        UserShortDto user;
        EventShortForRequestDto event;
        try {
            user = userClient.getById(userId);
            event = eventClient.getById(eventId);
        } catch (Exception e) {
            log.error("Feign client error while fetching user or event: {}", e.getMessage());
            throw new NotFoundException("Не удалось найти пользователя или событие");
        }

        if (user == null || event == null) {
            log.error("User or event not found after Feign call. User: {}, Event: {}", user, event);
            throw new NotFoundException("Не удалось найти пользователя или событие");
        }

        if (!Objects.equals(user.getId(), event.getOwnerId())) {
            log.warn("Access denied: User {} is not owner of event {}", userId, eventId);
            throw new ForbiddenException("User с id " + userId + " не владелец события " + eventId);
        }

        List<ParticipationRequest> requests;
        try {
            requests = requestRepository.findAllByIdIn(requestDto.getRequestIds());
        } catch (Exception e) {
            log.error("Database error while fetching requests by IDs: {}", e.getMessage());
            throw new ConflictException("Ошибка при получении заявок из базы данных");
        }

        if (requests.isEmpty()) {
            log.warn("No requests found for IDs: {}", requestDto.getRequestIds());
            throw new ConflictException("Заявки с указанными ID не найдены");
        }

        boolean hasPendingRequests = requests.stream().anyMatch(r -> r.getStatus() == RequestStatus.PENDING);
        if (!hasPendingRequests) {
            log.warn("No pending requests found among: {}", requests.stream().map(ParticipationRequest::getId).toList());
            throw new ConflictException("Нет pending-запросов для обновления");
        }

        Long confirmedCount;
        try {
            confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        } catch (Exception e) {
            log.error("Database error while counting confirmed requests: {}", e.getMessage());
            confirmedCount = 0L;
        }

        RequestStatusUpdateResponse response = new RequestStatusUpdateResponse(new HashSet<>(), new HashSet<>());

        try {
            if (!event.getIsModerated() || event.getParticipantLimit() == 0) {
                log.info("Auto-confirming all requests for event {}", eventId);
                for (ParticipationRequest request : requests) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    response.getConfirmedRequests().add(RequestMapper.fromRequestToRequestDto(request));
                }
            } else if (requestDto.getStatus() == CONFIRMED) {
                log.info("Manually confirming requests for event {}. Confirmed: {}, Limit: {}", eventId, confirmedCount, event.getParticipantLimit());
                if (confirmedCount >= event.getParticipantLimit()) {
                    log.warn("Participant limit reached for event {}", eventId);
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
                log.info("Manually rejecting requests for event {}", eventId);
                for (ParticipationRequest request : requests) {
                    if (request.getStatus() == RequestStatus.CONFIRMED) {
                        log.warn("Attempt to reject a confirmed request: {}", request.getId());
                        throw new ConflictException("Нельзя отклонить уже подтвержденную заявку");
                    }
                    if (request.getStatus() == RequestStatus.PENDING) {
                        request.setStatus(RequestStatus.REJECTED);
                        response.getRejectedRequests().add(RequestMapper.fromRequestToRequestDto(request));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during status update logic: {}", e.getMessage());
            throw new ConflictException("Ошибка при обновлении статусов заявок");
        }

        try {
            log.info("Saving {} requests to database", requests.size());
            requestRepository.saveAll(requests);
        } catch (Exception e) {
            log.error("Database error while saving requests: {}", e.getMessage());
            throw new ConflictException("Ошибка при сохранении заявок в базу данных");
        }

        log.info("END updateRequest. Returning response: {}", response);
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