package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.event.EventClient;
import ru.yandex.practicum.client.user.UserClient;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResponse;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.mapper.RequestMapper;
import ru.yandex.practicum.model.ParticipationRequest;
import ru.yandex.practicum.repository.RequestRepository;
import java.time.LocalDateTime;
import java.util.*;

import static ru.yandex.practicum.enums.RequestStatus.*;

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

    public List<RequestDto> getEventRequests(Long userId, Long eventId) {
        userClient.getUser(userId);
        EventDto eventDto = eventClient.getPublicEvent(eventId);
        if (!Objects.equals(eventDto.getInitiator().getId(), userId)) {
            throw new NotFoundException("User c id " + userId + " не хозяин для события " + eventId);
        }
        List<ParticipationRequest> result = requestRepository.findByEvent(eventId);

        return result.stream().map(RequestMapper::fromRequestTpRequestDto).toList();
    }

    public RequestStatusUpdateResponse updateRequest(Long userId, Long eventId, RequestStatusUpdateRequest request) {
        userClient.getUser(userId);
        EventDto eventDto = eventClient.getPublicEvent(eventId);
        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(request.getRequestIds());
        Set<RequestDto> confirmed = new HashSet<>();
        Set<RequestDto> rejected = new HashSet<>();
        RequestStatusUpdateResponse result = new RequestStatusUpdateResponse(confirmed, rejected);
        List<ParticipationRequest> pendingRequests = requests.stream()
                .filter(request1 -> request1.getStatus().equals(PENDING)).toList();

        if (pendingRequests.isEmpty()) {
            throw new ConflictException("Запрос не найден");
        }
        long confirmedRequestsCount = requestRepository.findAllByEventAndStatus(eventId, CONFIRMED).size();
        if (!eventDto.getRequestModeration() || eventDto.getParticipantLimit() == 0) {
            requests.forEach(req -> req.setStatus(CONFIRMED));
            result.getConfirmedRequests().addAll(requests.stream()
                    .map(RequestMapper::fromRequestTpRequestDto)
                    .toList());
            requestRepository.saveAll(requests);

            return result;
        }
        if ((confirmedRequestsCount + request.getRequestIds().size()) > eventDto.getParticipantLimit()) {
            throw new ConflictException("Для события с id " + eventId + " достигнут лимит участников");
        }

        if ((confirmedRequestsCount + request.getRequestIds().size()) == eventDto.getParticipantLimit() &&
                request.getStatus().equals(CONFIRMED)) {
            requests.forEach(req -> req.setStatus(REJECTED));
            confirmed.addAll(requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
            requestRepository.saveAll(requests);
            result.setConfirmedRequests(confirmed);

            List<ParticipationRequest> otherPendingRequests = requestRepository
                    .findAllByEventAndStatus(eventDto.getId(), PENDING);
            otherPendingRequests.forEach(req -> req.setStatus(REJECTED));
            requestRepository.saveAll(otherPendingRequests);
            rejected.addAll(otherPendingRequests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
            result.setRejectedRequests(rejected);
            return result;
        }

        if (request.getStatus().equals(CONFIRMED)) {
            requests.forEach(req -> req.setStatus(CONFIRMED));
            confirmed.addAll(requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
            requestRepository.saveAll(requests);
            result.setConfirmedRequests(confirmed);
        } else if (request.getStatus().equals(REJECTED)) {
            requests.forEach(req -> req.setStatus(REJECTED));
            rejected.addAll(requests.stream().map(RequestMapper::fromRequestTpRequestDto).toList());
            requestRepository.saveAll(requests);
            result.setRejectedRequests(rejected);
        }

        return result;
    }

    public List<RequestDto> getByUserId(Long userId) {
        List<ParticipationRequest> result = requestRepository.findAllByRequester(userClient.getUser(userId).getId());

        return result.stream().map(RequestMapper::fromRequestTpRequestDto).toList();

    }

    public RequestDto create(Long userId, Long eventId) {
        UserShortDto userDto = userClient.getUser(userId);
        EventDto eventDto = eventClient.getPublicEvent(eventId);
        if (Objects.equals(userDto.getId(), eventDto.getInitiator().getId())) {
            throw new ConflictException("User c id " + userId + " не хозяин для события " + eventId);
        }
        if (eventDto.getPublishedOn() == null) {
            throw new ConflictException("Событие с id " + eventId + " еще не опубликовано");
        }

        long confirmedRequestsCount = requestRepository.findAllByEventAndStatus(eventDto.getId(), CONFIRMED).size();
        if (confirmedRequestsCount == eventDto.getParticipantLimit() && eventDto.getParticipantLimit() > 0) {
            throw new ConflictException(
                    "Лимит участников для события " + eventId + " превышен");
        }
        if (!requestRepository.findAllByEventAndRequester(eventId, userId).isEmpty()) {
            throw new ConflictException("Запрос на участие в событии " + eventId +
                    " уже существует для пользователя с id " + userId);
        }
        ParticipationRequest request = new ParticipationRequest(null, LocalDateTime.now(), eventId, userId, PENDING);
        if (!eventDto.getRequestModeration()) {
            request.setStatus(CONFIRMED);
        }
        if (eventDto.getParticipantLimit() == 0) {
            request.setStatus(CONFIRMED);
        }
        ParticipationRequest result = requestRepository.save(request);

        return RequestMapper.fromRequestTpRequestDto(result);
    }

    public Map<Long, Long> getConfirmedRequestsCountForEvents(List<Long> eventIds) {
        List<ParticipationRequest> requests = requestRepository.findAllByEventInAndStatus(eventIds,
                RequestStatus.CONFIRMED);
        Set<Long> requestsIds = new HashSet<>();
        for (var request : requests) {
            requestsIds.add(request.getEventId());
        }
        Map<Long, Long> confirmedRequestsCountForEvents = new HashMap<>();
        for (var id : requestsIds) {
            int count = (int) requests.stream()
                    .filter(k -> Objects.equals(k.getEventId(), id)).count();
            confirmedRequestsCountForEvents.put(id, (long) count);
        }

        return confirmedRequestsCountForEvents;
    }

    public RequestDto cancelRequestByUser(Long userId, Long requestId) {
        UserShortDto userDto = userClient.getUser(userId);
        ParticipationRequest request = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не существует!"));
        if (!Objects.equals(request.getRequesterId(), userDto.getId())) {
            throw new ConflictException("User c id " + userId + " не хозяин для запроса " + requestId);
        }
        request.setStatus(CANCELED);
        ParticipationRequest result = requestRepository.save(request);

        return RequestMapper.fromRequestTpRequestDto(result);
    }
}