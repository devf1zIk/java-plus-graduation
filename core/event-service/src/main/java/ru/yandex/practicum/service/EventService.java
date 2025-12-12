package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.StatsClient;
import ru.yandex.practicum.client.request.RequestClient;
import ru.yandex.practicum.client.user.UserClient;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.AdminEventAction;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.exception.model.BadRequestException;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.mapper.*;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.EventCategoryRepository;
import ru.yandex.practicum.repository.EventRepository;
import ru.yandex.practicum.repository.LocationRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final StatsClient statsClient;

    public EventDto create(CreateNewEventDto eventDto, Long userId) {
        UserShortDto ownerId = userClient.getUser(userId);
        EventCategory category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new NotFoundException(
                        "Категория с id " + eventDto.getCategory() + "не существует!"));
        if (eventDto.getEventDate() != null &&
                eventDto.getEventDate().isBefore(now())) {
            throw new BadRequestException("Неверный eventStarDate: " + eventDto.getEventDate());
        }

        Event event = EventMapper.fromCreateNewEventDtoToEvent(eventDto, ownerId.getId(), category);
        if (event.getLocation().getLat() != null && event.getLocation().getLon() != null) {
            event.setLocation(saveLocation(event.getLocation()));
        } else {
            event.setLocation(saveLocation(new Location(-1L, 0.0, 0.0)));
        }
        if (event.getIsPaid() == null) {
            event.setIsPaid(false);
        }
        if (event.getParticipantLimit() == null) {
            event.setParticipantLimit(0L);
        }
        if (event.getIsModerated() == null) {
            event.setIsModerated(true);
        }
        Event result = eventRepository.save(event);

        return EventMapper.fromEventToEventDto(result, EventCategoryMapper.toCategoryDtoFromCategory(category),
                userClient.getUser(ownerId.getId()), 0L, 0);
    }

    public EventDto updateByAdmin(Long eventId, UpdateEventAdminDto updateEventDto) {
        Event event = getEventIfExist(eventId);
        if (!event.getState().equals(EventState.PENDING)) {
            throw new ConflictException("Только событие в статусе pending может быть опубликовано");
        }
        if (event.getPublishedOn() != null && updateEventDto.getStateAction().equals(AdminEventAction.REJECT_EVENT)) {
            throw new ConflictException("Неверный статус события");
        }
        if (updateEventDto.getEventDate() != null && updateEventDto.getEventDate().isBefore(now())) {
            throw new BadRequestException("Неверный eventStarDate: " + updateEventDto.getEventDate());
        }

        if (updateEventDto.getCategory() != null) {
            EventCategory category = categoryRepository
                    .findById(updateEventDto.getCategory())
                    .orElseThrow(() -> new NotFoundException(
                            "Категория с id " + updateEventDto.getCategory() + "не существует"));
            event.setCategory(category);
        }
        if (updateEventDto.getAnnotation() != null) {
            event.setAnnotation(updateEventDto.getAnnotation());
        }
        if (updateEventDto.getDescription() != null) {
            event.setDescription(updateEventDto.getDescription());
        }
        updateEvent(event, updateEventDto.getEventDate(), LocationMapper.location(updateEventDto.getLocation()), updateEventDto.getPaid(),
                updateEventDto.getParticipantLimit(), updateEventDto.getRequestModeration());
        if (updateEventDto.getStateAction() != null) {
            switch (updateEventDto.getStateAction()) {
                case PUBLISH_EVENT:
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(now().toInstant(ZoneOffset.UTC));
                    break;
                case REJECT_EVENT:
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ConflictException("Неверный статус события");
            }
        }
        if (updateEventDto.getTitle() != null) {
            event.setTitle(updateEventDto.getTitle());
        }
        Location location = event.getLocation();
        Location savedLOcation = saveLocation(location);
        event.setLocation(savedLOcation);
        Event updatedEvent = eventRepository.save(event);

        return getEventDtoFromEvent(updatedEvent);
    }

    public List<EventDto> getAll(List<Long> users, List<String> states, List<Long> categories,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd, Pageable pageable) {
        List<EventState> eventStates = new ArrayList<>();
        if (states != null) {
            for (String state : states) {
                if (!state.equals(EventState.PUBLISHED.toString()) &&
                        !state.equals(EventState.CANCELED.toString()) &&
                        !state.equals(EventState.PENDING.toString())) {
                    throw new BadRequestException("Неверный статус события: " + state);
                }
                eventStates.add(EventState.valueOf(state));
            }
        }

        List<Long> categoriesIds;
        if (categories == null || categories.isEmpty()) {
            categoriesIds = categoryRepository.findAll().stream()
                    .map(EventCategory::getId)
                    .collect(Collectors.toList());
        } else {
            categoriesIds = categories;
        }
        Page<Event> events;
        if (rangeStart == null || rangeEnd == null) {
            events = eventRepository.findAllEventsAfterDateForUsersByStateAndCategories(
                    users, eventStates, categoriesIds,
                    now().toInstant(ZoneOffset.UTC), pageable);
        } else {
            events = eventRepository.findAllEventsBetweenDatesForUsersByStateAndCategories(
                    users, eventStates, categoriesIds,
                    rangeStart.toInstant(ZoneOffset.UTC),
                    rangeEnd.toInstant(ZoneOffset.UTC),
                    pageable);
        }

        return getEventsFulls(events.getContent());
    }

    public EventDto getById(Long eventId) {
        Event event = getEventIfExist(eventId);
        if (event.getPublishedOn() == null) {
            throw new NotFoundException("Событие с id" + eventId + " ещё не опубликовано");
        }
        Integer views = getEventsViews(event.getId()) + 1;
        EventDto eventDto = getEventDtoFromEvent(event);
        eventDto.setViews(views);

        return eventDto;
    }

    public Integer getEventsViews(Long eventId) {
        List<String> uris = List.of("/events/" + eventId);
        List<HashMap<Object, Object>> stats = getStats(uris);
        if (stats != null && !stats.isEmpty()) {
            return (Integer) stats.getFirst().get("hits");
        } else {
            return 0;
        }
    }

    public EventDto updateByUser(UpdateEventUserRequest eventDto, Long userId, Long eventId) {
        Event event = getEventIfExist(eventId);
        userClient.getUser(userId);

        if (!Objects.equals(event.getOwnerId(), userId)) {
            throw new NotFoundException("User с id " + userId + " не хозяин для события " + eventId);
        }

        if (!event.getState().equals(EventState.PENDING) && !event.getState().equals(EventState.CANCELED)) {
            throw new ConflictException("Можно изменить только события в статусе pending или canceled");
        }

        if (eventDto.getEventDate() != null && eventDto.getEventDate().isBefore(now())) {
            throw new BadRequestException("Неверный eventStarDate: " + eventDto.getEventDate());
        }
        if (eventDto.getCategory() != null) {
            EventCategory category = categoryRepository
                    .findById(eventDto.getCategory())
                    .orElseThrow(() -> new NotFoundException(
                            "Категория с id " + eventDto.getCategory() + "не существует в БД"));
            event.setCategory(category);
        }
        if (eventDto.getAnnotation() != null && !event.getAnnotation().isBlank()) {
            event.setAnnotation(eventDto.getAnnotation());
        }

        if (eventDto.getDescription() != null && !eventDto.getDescription().isBlank()) {
            event.setDescription(eventDto.getDescription());
        }
        updateEvent(event, eventDto.getEventDate(), LocationMapper.location(eventDto.getLocation()), eventDto.getPaid(),
                eventDto.getParticipantLimit(), eventDto.getRequestModeration());
        if (eventDto.getStateAction() != null) {
            switch (eventDto.getStateAction()) {
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                default:
                    throw new ConflictException("Неверный статус события");
            }
        }
        if (eventDto.getTitle() != null && !eventDto.getTitle().isBlank()) {
            event.setTitle(eventDto.getTitle());
        }

        return getEventDtoFromEvent(event);
    }

    public List<EventShortDto> getAllShort(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           boolean onlyAvailable, String sort, int from, int size) {

        Page<Event> events;
        Pageable paging;

        if (sort == null || sort.isBlank()) {
            paging = PageRequest.of(from / size, size);
        } else {
            if (sort.equals("EVENT_DATE")) {
                paging = PageRequest.of(from / size, size, Sort.by("eventDateTime").descending());
            } else if (sort.equals("VIEWS")) {
                paging = PageRequest.of(from / size, size, Sort.by("views").descending());
            } else {
                throw new ConflictException("Неверная сортировка. Используйте VIEWS или EVENT_DATE");
            }
        }
        if (onlyAvailable) {
            if (rangeStart == null || rangeEnd == null) {
                events = eventRepository.findAllAvailablePublishedEventsByCategoryAndStateAfterDate(
                        text, now().toInstant(ZoneOffset.UTC),
                        categories, paging, EventState.PUBLISHED, paid);
            } else {
                events = eventRepository.findAllAvailablePublishedEventsByCategoryAndStateBetweenDates(
                        text, rangeStart.toInstant(ZoneOffset.UTC),
                        rangeEnd.toInstant(ZoneOffset.UTC),
                        categories, paging, EventState.PUBLISHED, paid);
            }
        } else {
            if (rangeStart == null || rangeEnd == null) {
                events = eventRepository.findAllEventsWithStatusAfterDate(
                        text, now().toInstant(ZoneOffset.UTC),
                        categories, EventState.PUBLISHED, paging, paid);
            } else {
                events = eventRepository.findAllEventsWithStatusBetweenDates(
                        text, rangeStart.toInstant(ZoneOffset.UTC),
                        rangeEnd.toInstant(ZoneOffset.UTC),
                        categories, EventState.PUBLISHED, paging, paid);
            }
        }

        return getEventsShorts(events.getContent());
    }

    public List<EventShortDto> getByUserId(Long userId, Pageable paging) {
        UserShortDto user = userClient.getUser(userId);
        List<Event> events = eventRepository.findAllByOwnerId(user.getId(), paging).stream().toList();

        return getEventsShorts(events);
    }

    public EventDto getEventByUserId(Long userId, Long eventId) {
        Event event = getEventIfExist(eventId);
        userClient.getUser(userId);
        if (!Objects.equals(event.getOwnerId(), userId)) {
            throw new NotFoundException("User с id " + userId + " не хозяин события " + eventId);
        }

        return getEventDtoFromEvent(event);
    }

    public Event getEventIfExist(long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не существует!"));
    }

    public Map<Long, Integer> getEventsViewsMap(List<Long> eventsIds) {
        List<String> uris = new ArrayList<>();
        for (Long eventId : eventsIds) {
            uris.add("/events/" + eventId);
        }
        List<HashMap<Object, Object>> stats = getStats(uris);
        Map<Long, Integer> eventViewsMap = new HashMap<>();
        if (stats != null && !stats.isEmpty()) {
            for (var map : stats) {
                String uri = (String) map.get("uri");
                String[] urisAsArr = uri.split("/");
                Long id = Long.parseLong(urisAsArr[urisAsArr.length - 1]);
                eventViewsMap.put(id, (Integer) map.get("hits"));
            }
        }
        for (Long id : eventsIds) {
            if (!eventViewsMap.containsKey(id)) {
                eventViewsMap.put(id, 0);
            }
        }

        return eventViewsMap;
    }

    private Location saveLocation(Location location) {
        Optional<Location> existedLocation = locationRepository
                .findByLatAndLon(location.getLat(), location.getLon());
        if (existedLocation.isEmpty()) {
            locationRepository.save(location);
        }

        return locationRepository.findByLatAndLon(location.getLat(), location.getLon())
                .orElse(new Location());
    }

    private void updateEvent(Event event, LocalDateTime eventDate, Location location, Boolean paid, Long
            participantLimit, Boolean requestModeration) {
        if (eventDate != null) {
            event.setEventDateTime(eventDate.toInstant(ZoneOffset.UTC));
        }
        if (location != null) {
            event.setLocation(location);
        }
        if (paid != null) {
            event.setIsPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setIsModerated(requestModeration);
        }
    }

    private EventDto getEventDtoFromEvent(Event event) {
        long confirmedRequests = eventRepository.findAllByEventInAndStatus(Collections.singletonList(event.getId()), RequestStatus.CONFIRMED).size();
        Integer views = getEventsViews(event.getId());

        return EventMapper.fromEventToEventDto(event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                userClient.getUser(event.getOwnerId()),
                confirmedRequests,
                views);
    }

    private List<HashMap<Object, Object>> getStats(List<String> uris) {
        return (List<HashMap<Object, Object>>) statsClient.getStats("2000-01-01 00:00:00",
                now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                uris, false).getBody();
    }

    private List<Long> getEventsIdFromEventsList(List<Event> events) {
        return events.stream().map(Event::getId).toList();
    }

    private List<EventDto> getEventsFulls(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedRequestsCountForEvents = requestClient
                .getConfirmedRequestsCount(eventIds, RequestStatus.CONFIRMED);

        Map<Long, Integer> viewsMap = getEventsViewsMap(eventIds);

        return events.stream()
                .map(event -> EventMapper.fromEventToEventDto(event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        userClient.getUser(event.getOwnerId()),
                        confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0)))
                .collect(Collectors.toList());
    }

    private List<EventShortDto> getEventsShorts(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedRequestsCountForEvents = requestClient
                .getConfirmedRequestsCount(eventIds, RequestStatus.CONFIRMED);

        Map<Long, Integer> viewsMap = getEventsViewsMap(eventIds);

        return events.stream()
                .map(event -> EventMapper.fromEventToEventShortDto(event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        userClient.getUser(event.getOwnerId()),
                        confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0)))
                .collect(Collectors.toList());
    }
}