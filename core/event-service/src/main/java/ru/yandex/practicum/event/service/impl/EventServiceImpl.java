package ru.yandex.practicum.event.service.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.ewm.client.stats.CollectorClient;
import ru.practicum.ewm.client.stats.RecommendationsClient;
import ru.practicum.grpc.stats.messages.ActionTypeProto;
import ru.practicum.grpc.stats.messages.RecommendedEventProto;
import ru.yandex.practicum.event.mapper.EventMapper;
import ru.yandex.practicum.event.model.Category;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.model.Location;
import ru.yandex.practicum.event.model.QEvent;
import ru.yandex.practicum.event.repository.CategoryRepository;
import ru.yandex.practicum.event.repository.EventRepository;
import ru.yandex.practicum.event.service.EventService;
import ru.yandex.practicum.interaction.client.ParticipationRequestClient;
import ru.yandex.practicum.interaction.client.UserClient;
import ru.yandex.practicum.interaction.dto.*;
import ru.yandex.practicum.interaction.dto.params.EventParamsAdmin;
import ru.yandex.practicum.interaction.dto.params.EventParamsPublic;
import ru.yandex.practicum.interaction.dto.params.UserParamsAdmin;
import ru.yandex.practicum.interaction.enums.EventState;
import ru.yandex.practicum.interaction.enums.ParticipationRequestStatus;
import ru.yandex.practicum.interaction.enums.RequestStatus;
import ru.yandex.practicum.interaction.exception.ConflictException;
import ru.yandex.practicum.interaction.exception.NotFoundException;
import ru.yandex.practicum.interaction.exception.ValidationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ParticipationRequestClient requestClient;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final RecommendationsClient recommendationsClient;
    private final CollectorClient collectorClient;


    @Override
    public List<EventShortDto> getUserEvents(Long userId, UserParamsAdmin params) {
        log.info("EventServiceImpl - getUserEvents");
        int from = params.getFrom();
        int size = params.getSize();

        PageRequest page = PageRequest.of(from / size, size);
        UserShortDto userShortDto = userClient.getUserById(userId);

        BooleanExpression byUserId = QEvent.event.initiatorId.eq(userId);
        List<Event> events = eventRepository.findAll(byUserId, page).getContent();

        Map<Long, Double> ratingMap = getRating(events);
        Map<Long, Long> confrmedMap = getConfirmedRequests(events);

        return events
                .stream()
                .map(event -> EventMapper
                        .toShortDto(event,
                                confrmedMap.get(event.getId()),
                                ratingMap.get(event.getId()),
                                userShortDto.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        UserShortDto userShortDto = userClient.getUserById(userId);

        Event event = getEventById(eventId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not the owner of this event");
        }

        Map<Long, Double> ratingMap = getRating(List.of(event));
        Map<Long, Long> confrmedMap = getConfirmedRequests(List.of(event));

        return EventMapper.entityToFullDto(event, confrmedMap.get(event.getId()), ratingMap.get(event.getId()), userShortDto.getName());
    }

    @Override
    @Transactional
    public EventFullDto createUserEvent(Long userId, NewEventDto dto) {

        UserShortDto userShortDto = userClient.getUserById(userId);

        categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("The category with id: " + dto.getCategory() + " not found!"));

        if (dto.getEventDate().isBefore(LocalDateTime.now()) ||
                !dto.getEventDate().isAfter(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("The date and time of the event cannot be in the past " +
                    "or earlier than two hours from now: " + dto.getEventDate());
        }

        Event event = EventMapper.toEntity(dto, userId);
        Event savedEvent = eventRepository.save(event);

        return EventMapper.entityToFullDto(savedEvent, 0, 0, userShortDto.getName());
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {

        Event event = getEventById(eventId);
        UserShortDto userShortDto = userClient.getUserById(userId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not the owner of this event");
        }

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("A published event cannot be modified.");
        }

        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());

        if (dto.getEventDate() != null) {
            LocalDateTime newEventDate = LocalDateTime.parse(dto.getEventDate().replace(" ", "T"));
            if (newEventDate.isBefore(LocalDateTime.now()) ||
                    !newEventDate.isAfter(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("The date and time of the event cannot be in the past " +
                        "or earlier than two hours from now: " + dto.getEventDate());
            }
            event.setEventDate(newEventDate);
        }

        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("The category with id: " + dto.getCategory() +
                            " not found!"));
            event.setCategory(category);
        }

        if ("SEND_TO_REVIEW".equals(dto.getStateAction())) {
            event.setState(EventState.PENDING);
        } else if ("CANCEL_REVIEW".equals(dto.getStateAction())) {
            event.setState(EventState.CANCELED);
        }

        Map<Long, Double> ratingMap = getRating(List.of(event));
        Map<Long, Long> confirmedMap = getConfirmedRequests(List.of(event));

        eventRepository.save(event);

        return EventMapper.entityToFullDto(event, confirmedMap.get(event.getId()), ratingMap.get(event.getId()), userShortDto.getName());
    }

    @Override
    public List<ParticipationRequestDto> getAllParticipationRequestsByUserIdAndEventId(Long userId, Long eventId) {
        Event event = getEventById(eventId);
        userClient.getUserById(userId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not the owner of this event");
        }

        return requestClient.getAllByEventId(eventId);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest requestUpdate) {
        Event event = getEventById(eventId);
        userClient.getUserById(userId);

        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not the owner of this event");
        }

        List<Long> requestIds = requestUpdate.getRequestIds();
        RequestStatus status = requestUpdate.getStatus();
        int requestCount = requestIds.size();
        int limit = event.getParticipantLimit();

        List<ParticipationRequestDto> requests = requestClient.getAllByEventId(eventId);

        long currentConfirmed = requestClient.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        if (currentConfirmed == -1L) {
            throw new RuntimeException("Updating is not possible now: Request service is unavailable, please try again later.");
        }

        if (currentConfirmed == limit) {
            throw new ConflictException("The request limit for this event has been reached: " + event);
        }

        requests
                .stream()
                .filter(request -> !request.getStatus()
                        .equals(ParticipationRequestStatus.PENDING))
                .forEach(request -> {
                    throw new ConflictException("The status can only be changed for requests that are in the pending " +
                            "state. The request has the status: " + request.getStatus());
                });

        List<ParticipationRequestDto> updatedRequests = new ArrayList<>();
        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        switch (status) {
            case CONFIRMED: {
                if (limit == 0 || !event.isRequestModeration() || currentConfirmed + requestCount <= limit) {
                    for (ParticipationRequestDto request : requests) {
                        request.setStatus(ParticipationRequestStatus.CONFIRMED);
                        updatedRequests.add(request);
                        confirmedRequests.add(request);
                    }
                } else if (currentConfirmed >= limit) {
                    throw new ConflictException("The request limit for this event has been reached: " + event);
                } else {
                    for (ParticipationRequestDto request : requests) {
                        if (limit > currentConfirmed) {
                            request.setStatus(ParticipationRequestStatus.CONFIRMED);
                            updatedRequests.add(request);
                            confirmedRequests.add(request);
                            currentConfirmed = currentConfirmed + 1;
                        } else {
                            request.setStatus(ParticipationRequestStatus.REJECTED);
                            updatedRequests.add(request);
                            rejectedRequests.add(request);
                        }
                    }
                }
                break;
            }
            case REJECTED: {
                for (ParticipationRequestDto request : requests) {
                    request.setStatus(ParticipationRequestStatus.REJECTED);
                    updatedRequests.add(request);
                    rejectedRequests.add(request);
                }
            }
            break;
        }

        int isUpdate = requestClient.updateAllRequests(updatedRequests);

        log.info("Обновлено заявок, шт: {}", isUpdate);

        if (isUpdate == -1) {
            throw new RuntimeException("Updating is not possible now: Request service is unavailable, please try again later.");
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmedRequests);
        result.setRejectedRequests(rejectedRequests);

        return result;
    }


    @Override
    public List<EventShortDto> getPublicEvents(EventParamsPublic params, HttpServletRequest request) {
        PageRequest page = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        BooleanBuilder where = new BooleanBuilder();
        QEvent event = QEvent.event;

        String text = params.getText();
        List<Long> categories = params.getCategories();
        boolean onlyAvailable = params.isOnlyAvailable();
        String sort = params.getSort();
        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;

        if (params.getRangeStart() != null) {
            rangeStart = LocalDateTime.parse(params.getRangeStart().replace(" ", "T"));
        }

        if (params.getRangeEnd() != null) {
            rangeEnd = LocalDateTime.parse(params.getRangeEnd().replace(" ", "T"));
        }

        where.and(event.state.in(EventState.PUBLISHED));

        if (text != null && !text.isEmpty()) {
            where.and(event.annotation.lower().like("%" + text.toLowerCase() + "%")
                    .or(event.description.lower().like("%" + text.toLowerCase() + "%")));
        }

        if (categories != null && !categories.isEmpty()) {
            if (categories.size() == 1 && categories.getFirst().equals(0L)) {
                throw new ValidationException("Incorrect list of category IDs: " + categories);
            }
            where.and(event.category.id.in(categories));
        }

        if (params.getPaid() != null) {
            where.and(event.paid.eq(params.getPaid()));
        }

        if (rangeStart != null) {
            where.and(event.eventDate.after(rangeStart));
        }

        if (rangeEnd != null) {
            where.and(event.eventDate.before(rangeEnd));
        }

        if (rangeStart == null && rangeEnd == null) {
            where.and(event.eventDate.after(LocalDateTime.now()));
        }

        List<Event> events = eventRepository.findAll(where, page).getContent();

        Map<Long, Long> confirmedMap = getConfirmedRequests(events);
        Map<Long, Double> ratingMap = getRating(events);
        Map<Long, String> initiatorNames = getInitiatorNames(events);


        if (onlyAvailable) {
            events = events
                    .stream()
                    .filter(e -> e.getParticipantLimit() > confirmedMap.getOrDefault(e.getId(), 0L))
                    .toList();
        }

        List<EventShortDto> eventShorts = events
                .stream()
                .map(e -> EventMapper.toShortDto(
                        e,
                        confirmedMap.getOrDefault(e.getId(), 0L),
                        ratingMap.getOrDefault(e.getId(), (double) 0),
                        initiatorNames.getOrDefault(e.getId(), null)))
                .collect(Collectors.toList());


        if (sort == null) {
            return eventShorts;
        }

        return switch (sort) {
            case "VIEWS" -> eventShorts
                    .stream()
                    .sorted(Comparator.comparingDouble(EventShortDto::getRating).reversed())
                    .collect(Collectors.toList());
            case "EVENT_DATE" -> eventShorts
                    .stream()
                    .sorted(Comparator.comparing(EventShortDto::getEventDate))
                    .collect(Collectors.toList());
            default -> eventShorts;
        };
    }

    @Override
    @Transactional
    public EventFullDto getEventByIdAndLogHit(Long eventId, long userId) {
        Event event = getEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event is not published");
        }

        Map<Long, Double> ratingMap = getRating(List.of(event));
        Map<Long, Long> confirmedMap = getConfirmedRequests(List.of(event));
        Map<Long, String> initiatorNames = getInitiatorNames(List.of(event));


        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);


        return EventMapper.entityToFullDto(event,
                confirmedMap.get(event.getId()), ratingMap.get(event.getId()), initiatorNames.get(event.getId()));
    }

    @Override
    @Transactional
    public EventFullDto getEventForInternalUse(Long eventId) { //для взаимодействия с другими микросервисами
        Event event = getEventById(eventId);
        Map<Long, Double> ratingMap = getRating(List.of(event));
        Map<Long, Long> confirmedMap = getConfirmedRequests(List.of(event));
        Map<Long, String> initiatorNames = getInitiatorNames(List.of(event));
        return EventMapper.entityToFullDto(event,
                confirmedMap.get(event.getId()), ratingMap.get(event.getId()), initiatorNames.get(event.getId()));
    }


    @Override
    public List<EventFullDto> getEventsByAdmin(EventParamsAdmin params) {
        PageRequest page = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        BooleanBuilder where = new BooleanBuilder();
        QEvent event = QEvent.event;

        List<Long> users = params.getUsers();
        List<Long> categories = params.getCategories();
        LocalDateTime rangeStart = null;
        LocalDateTime rangeEnd = null;

        if (params.getStates() != null && !params.getStates().isEmpty()) {
            List<EventState> states = params.getStates().stream().map(EventState::valueOf).collect(Collectors.toList());
            where.and(event.state.in(states));
        }

        if (users != null && !users.isEmpty()) {
            if (users.size() == 1 && users.getFirst() == 0L) {
                throw new ValidationException("Incorrect list of category IDs: " + categories);
            }
            where.and(event.initiatorId.in(users));
        }

        if (categories != null && !categories.isEmpty()) {
            if (categories.size() == 1 && categories.getFirst() == 0L) {
                throw new ValidationException("Incorrect list of category IDs: " + categories);
            }
            where.and(event.category.id.in(categories));
        }

        if (params.getRangeStart() != null) {
            rangeStart = LocalDateTime.parse(params.getRangeStart().replace(" ", "T"));
        }

        if (params.getRangeEnd() != null) {
            rangeEnd = LocalDateTime.parse(params.getRangeEnd().replace(" ", "T"));
        }

        if (rangeStart != null) {
            where.and(event.eventDate.after(rangeStart));
        }

        if (rangeEnd != null) {
            where.and(event.eventDate.before(rangeEnd));
        }

        List<Event> events = eventRepository.findAll(where, page).getContent();

        Map<Long, Double> ratingMap = getRating(events);
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);
        Map<Long, String> initiatorNames = getInitiatorNames(events);

        return events
                .stream()
                .map(e -> EventMapper.entityToFullDto(
                        e,
                        confirmedMap.get(e.getId()),
                        ratingMap.get(e.getId()),
                        initiatorNames.get(e.getId())
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = getEventById(eventId);

        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id: " + dto.getCategory() + " not found!"));
            event.setCategory(category);
        }
        if (dto.getEventDate() != null) {
            LocalDateTime newEventDate = LocalDateTime.parse(dto.getEventDate().replace(" ", "T"));
            if (newEventDate.isBefore(LocalDateTime.now())) {
                throw new ValidationException("The event date cannot be in the past: " + dto.getEventDate());
            }
            event.setEventDate(newEventDate);
        }

        if (dto.getLocation() != null) {
            event.setLocation(new Location(dto.getLocation().getLat(), dto.getLocation().getLon()));
        }

        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());

        if ("PUBLISH_EVENT".equals(dto.getStateAction())) {
            if (!event.getState().equals(EventState.PENDING)) {
                throw new ConflictException("Event must be in PENDING state to publish");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if ("REJECT_EVENT".equals(dto.getStateAction())) {
            if (event.getState().equals(EventState.PUBLISHED)) {
                throw new ConflictException("Cannot reject a published event");
            }
            event.setState(EventState.CANCELED);
        }

        Event savedEvent = eventRepository.save(event);

        Map<Long, Double> ratingMap = getRating(List.of(savedEvent));
        Map<Long, Long> confirmedMap = getConfirmedRequests(List.of(savedEvent));
        Map<Long, String> initiatorNames = getInitiatorNames(List.of(event));

        return EventMapper
                .entityToFullDto(event,
                        confirmedMap.get(savedEvent.getId()),
                        ratingMap.get(savedEvent.getId()),
                        initiatorNames.get(savedEvent.getId())
                );
    }

    private Map<Long, Double> getRating(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        return recommendationsClient
                .getInteractionsCount(eventIds)
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        List<ParticipationRequestDto> requests = requestClient
                .getAllByEventIdsAndStatus(eventIds, ParticipationRequestStatus.CONFIRMED);

        Map<Long, List<ParticipationRequestDto>> result = requests
                .stream()
                .collect(Collectors.groupingBy(ParticipationRequestDto::getEvent));

        return eventIds
                .stream()
                .collect(Collectors
                        .toMap(eventId -> eventId,
                                eventId -> (long) result.getOrDefault(eventId,
                                        List.of()).size(), (a, b) -> b));
    }

    private Event getEventById(Long eventId) {
        BooleanExpression byEventId = QEvent.event.id.eq(eventId);
        return eventRepository.findOne(byEventId)
                .orElseThrow(() -> new NotFoundException("The event with id: " + eventId + " not found!"));
    }

    private Map<Long, String> getInitiatorNames(List<Event> events) {

        List<Long> initiatorsIds = events.stream().map(Event::getInitiatorId).distinct().toList();

        List<UserDto> initiators = userClient.getAll(new UserParamsAdmin(initiatorsIds, 0, 0));

        Map<Long, String> userIdToName = initiators.stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getName));

        return events.stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        event -> userIdToName.get(event.getInitiatorId())
                ));
    }

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        userClient.getUserById(userId);
        return recommendationsClient.getRecommendationsForUser(userId, maxResults).toList();
    }

    @Override
    public void sendLikeToCollector(long userId, long eventId) {

        userClient.getUserById(userId);
        Event event = getEventById(eventId);

        ParticipationRequestDto requestDto = requestClient.getByEventIdAndUserId(eventId, userId);
        if (requestDto.getStatus() != ParticipationRequestStatus.CONFIRMED) {
            throw new IllegalArgumentException("User can like only events they have participated in");
        }

        if (!event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("User can like only events that have already taken place");
        }

        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }
}