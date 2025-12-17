package ru.yandex.practicum.service;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.event.EventClient;
import ru.yandex.practicum.client.user.UserClient;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateRequest;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.exception.model.ServiceUnavailableException;
import ru.yandex.practicum.mapper.RequestMapper;
import ru.yandex.practicum.model.ParticipationRequest;
import ru.yandex.practicum.repository.RequestRepository;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResponse;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Autowired
    public RequestService(RequestRepository requestRepository, UserClient userClient, EventClient eventClient) {
        this.requestRepository = requestRepository;
        this.userClient = userClient;
        this.eventClient = eventClient;
    }

    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        try {
            EventDto eventDto = eventClient.getEvent(eventId);
            userClient.getUser(userId);

            if (!Objects.equals(eventDto.getInitiator().getId(), userId)) {
                throw new NotFoundException("User c id " + userId + " не хозяин для события " + eventId);
            }

            List<ParticipationRequest> result = requestRepository.findByEventId(eventId);
            return result.stream().map(RequestMapper::fromRequestToRequestDto).toList();
        } catch (ServiceUnavailableException e) {
            throw new ServiceUnavailableException("User or Event service is unavailable");
        }
    }

    public RequestStatusUpdateResponse updateRequest(Long userId, Long eventId, RequestStatusUpdateRequest request) {
        try {
            UserShortDto user = userClient.getUser(userId);
            if (user == null) {
                throw new ServiceUnavailableException("User service is unavailable");
            }
            EventDto eventDto = eventClient.getEvent(eventId);
            if (eventDto == null) {
                throw new ServiceUnavailableException("Event service is unavailable");
            }
            if (!Objects.equals(eventDto.getInitiator().getId(), userId)) {
                throw new NotFoundException("User с id " + userId + " не является владельцем события " + eventId);
            }

            List<ParticipationRequest> requests = requestRepository.findAllByIdIn(request.getRequestIds());
            Set<ParticipationRequestDto> confirmed = new HashSet<>();
            Set<ParticipationRequestDto> rejected = new HashSet<>();
            RequestStatusUpdateResponse result = new RequestStatusUpdateResponse(confirmed, rejected);
            List<ParticipationRequest> pendingRequests = requests.stream()
                    .filter(req -> req.getStatus().equals(RequestStatus.PENDING))
                    .toList();

            if (pendingRequests.isEmpty()) {
                throw new ConflictException("Запрос не найден");
            }

            long confirmedRequestsCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (!eventDto.getRequestModeration() || eventDto.getParticipantLimit() == 0) {
                requests.forEach(req -> req.setStatus(RequestStatus.CONFIRMED));
                result.getConfirmedRequests().addAll(requests.stream()
                        .map(RequestMapper::fromRequestToRequestDto)
                        .toList());
                requestRepository.saveAll(requests);
                return result;
            }
            if ((confirmedRequestsCount + request.getRequestIds().size()) > eventDto.getParticipantLimit()) {
                throw new ConflictException("Для события с id " + eventId + " достигнут лимит участников");
            }

            if ((confirmedRequestsCount + request.getRequestIds().size()) == eventDto.getParticipantLimit() &&
                    request.getStatus().equals(RequestStatus.CONFIRMED)) {
                requests.forEach(req -> req.setStatus(RequestStatus.CONFIRMED));
                confirmed.addAll(requests.stream()
                        .map(RequestMapper::fromRequestToRequestDto)
                        .toList());
                requestRepository.saveAll(requests);
                result.setConfirmedRequests(confirmed);

                List<ParticipationRequest> otherPendingRequests = requestRepository
                        .findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);
                otherPendingRequests.forEach(req -> req.setStatus(RequestStatus.REJECTED));
                requestRepository.saveAll(otherPendingRequests);
                rejected.addAll(otherPendingRequests.stream()
                        .map(RequestMapper::fromRequestToRequestDto)
                        .toList());
                result.setRejectedRequests(rejected);
                return result;
            }

            if (request.getStatus().equals(RequestStatus.CONFIRMED)) {
                requests.forEach(req -> req.setStatus(RequestStatus.CONFIRMED));
                confirmed.addAll(requests.stream()
                        .map(RequestMapper::fromRequestToRequestDto)
                        .toList());
                requestRepository.saveAll(requests);
                result.setConfirmedRequests(confirmed);
            } else if (request.getStatus().equals(RequestStatus.REJECTED)) {
                requests.forEach(req -> req.setStatus(RequestStatus.REJECTED));
                rejected.addAll(requests.stream()
                        .map(RequestMapper::fromRequestToRequestDto)
                        .toList());
                requestRepository.saveAll(requests);
                result.setRejectedRequests(rejected);
            }

            return result;

        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (FeignException e) {
            throw new ServiceUnavailableException("External service is unavailable: " + e.getMessage());
        }
    }

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        try {
            UserShortDto user = userClient.getUser(userId);
            if (user == null) {
                throw new ServiceUnavailableException("User service is unavailable");
            }
            List<ParticipationRequest> result = requestRepository.findAllByRequesterId(user.getId());
            return result.stream().map(RequestMapper::fromRequestToRequestDto).toList();
        } catch (ServiceUnavailableException e) {
            throw new ServiceUnavailableException("User service is unavailable");
        }
    }

    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        try {
            UserShortDto user = userClient.getUser(userId);
            if (user == null) {
                throw new ServiceUnavailableException("User service is unavailable");
            }
            EventDto eventDto = eventClient.getEvent(eventId);
            if (eventDto == null) {
                throw new ServiceUnavailableException("Event service is unavailable");
            }
            if (Objects.equals(userId, eventDto.getInitiator().getId())) {
                throw new ConflictException("User с id " + userId + " является инициатором события " + eventId);
            }
            if (eventDto.getPublishedOn() == null) {
                throw new ConflictException("Событие с id " + eventId + " еще не опубликовано");
            }
            Long confirmedRequestsCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedRequestsCount == eventDto.getParticipantLimit() && eventDto.getParticipantLimit() > 0) {
                throw new ConflictException("Лимит участников для события " + eventId + " превышен");
            }
            if (!requestRepository.findAllByEventIdAndRequesterId(eventId, userId).isEmpty()) {
                throw new ConflictException("Запрос на участие в событии " + eventId +
                        " уже существует для пользователя с id " + userId);
            }
            ParticipationRequest request = new ParticipationRequest();
            request.setEventId(eventId);
            request.setRequesterId(userId);
            request.setCreatedOn(LocalDateTime.now());
            boolean autoConfirm = !eventDto.getRequestModeration() || eventDto.getParticipantLimit() == 0;
            request.setStatus(autoConfirm ? RequestStatus.CONFIRMED : RequestStatus.PENDING);
            ParticipationRequest result = requestRepository.save(request);

            return RequestMapper.fromRequestToRequestDto(result);
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (FeignException e) {
            throw new ServiceUnavailableException("External service is unavailable: " + e.getMessage());
        }
    }

    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        try {
            UserShortDto user = userClient.getUser(userId);
            if (user == null) {
                throw new ServiceUnavailableException("User service is unavailable");
            }

            ParticipationRequest request = requestRepository
                    .findById(requestId)
                    .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не существует!"));
            if (!Objects.equals(request.getRequesterId(), userId)) {
                throw new ConflictException("User c id " + userId + " не хозяин для запроса " + requestId);
            }
            request.setStatus(RequestStatus.CANCELED);
            ParticipationRequest result = requestRepository.save(request);
            return RequestMapper.fromRequestToRequestDto(result);
        } catch (ServiceUnavailableException e) {
            throw new ServiceUnavailableException("User service is unavailable");
        }
    }

    public Long countByEventIdAndStatus(Long eventId, RequestStatus status) {
        return requestRepository.countByEventIdAndStatus(eventId, status);
    }
}