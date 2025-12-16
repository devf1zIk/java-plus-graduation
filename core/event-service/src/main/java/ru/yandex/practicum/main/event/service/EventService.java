package ru.yandex.practicum.main.event.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.StatsClient;
import ru.yandex.practicum.client.request.RequestClient;
import ru.yandex.practicum.client.user.UserClient;
import ru.yandex.practicum.dto.event.NewEventDto;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.event.UpdateEventAdminDto;
import ru.yandex.practicum.enums.AdminEventAction;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.exception.model.BadRequestException;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.main.category.mapper.EventCategoryMapper;
import ru.yandex.practicum.main.category.model.EventCategory;
import ru.yandex.practicum.main.category.repository.EventCategoryRepository;
import ru.yandex.practicum.main.event.mapper.EventMapper;
import ru.yandex.practicum.main.event.mapper.LocationMapper;
import ru.yandex.practicum.main.event.model.Event;
import ru.yandex.practicum.main.event.model.Location;
import ru.yandex.practicum.main.event.repository.EventRepository;
import ru.yandex.practicum.main.event.repository.LocationRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static java.time.LocalDateTime.now;

@Service
public class EventService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final StatsClient statsClient;

    @Autowired
    public EventService(EventCategoryRepository categoryRepository, EventRepository eventRepository,
                        LocationRepository locationRepository, RequestClient requestClient,
                        UserClient userClient, StatsClient statsClient) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.requestClient = requestClient;
        this.userClient = userClient;
        this.statsClient = statsClient;
    }

    public EventDto create(NewEventDto eventDto, Long userId) {
        userClient.getUserShortById(userId);
        EventCategory category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new NotFoundException(
                        "Категория с id " + eventDto.getCategory() + "не существует!"));
        if (eventDto.getEventDate() != null &&
                eventDto.getEventDate().isBefore(now())) {
            throw new BadRequestException("Неверный eventStarDate: " + eventDto.getEventDate());
        }

        Event event = EventMapper.fromCreateNewEventDtoToEvent(eventDto, category);
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
                userClient.getUserShortById(userId), 0L, 0);
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
        updateEvent(event, updateEventDto.getEventDate(), LocationMapper.location(updateEventDto.getLocationDto()), updateEventDto.getPaid(),
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
                eventStates.add(EventState.valueOf(state));
            }
        } else {
            eventStates = null;
        }

        if (states != null) {
            for (String state : states) {
                if (!state.equals(EventState.PUBLISHED.toString()) && !state.equals(EventState.CANCELED.toString()) &&
                        !state.equals(EventState.PENDING.toString())) {
                    throw new BadRequestException("Неверный статус события");
                }
            }
        }
        List<Long> categoriesIds;
        if (categories == null || categories.isEmpty()) {
            categoriesIds = categoryRepository.findAll().stream().map(EventCategory::getId).toList();
        } else {
            categoriesIds = categories;
        }
        Page<Event> events;
        if (rangeStart == null || rangeEnd == null) {
            events = eventRepository.findAllEventsAfterDateForUsersByStateAndCategories(users, eventStates,
                    categoriesIds, now().toInstant(ZoneOffset.UTC), pageable);

        } else {
            events = eventRepository.findAllEventsBetweenDatesForUsersByStateAndCategories(users, eventStates,
                    categoriesIds, rangeStart.toInstant(ZoneOffset.UTC), rangeEnd.toInstant(ZoneOffset.UTC), pageable);
        }

        return getEventsFulls(events.stream().toList());
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

    public EventDto updateByUser(ru.yandex.practicum.dto.event.UpdateEventUserRequest eventDto, Long userId, Long eventId) {
        Event event = getEventIfExist(eventId);
        userClient.getUserShortById(userId);

        if (!Objects.equals(event.getInitiatorId(), userId)) {
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
        updateEvent(event, eventDto.getEventDate(), LocationMapper.location(eventDto.getLocationDto()), eventDto.getPaid(),
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
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd, boolean onlyAvailable,
                                           String sort, int from, int size) {
        Page<Event> events;
        Pageable paging;
        if (sort == null) {
            paging = PageRequest.of(from, size);
        } else {
            if (sort.equals("VIEWS") || sort.equals("EVENT_DATE") || sort.isBlank()) {
                if (sort.equals("EVENT_DATE")) {
                    paging = PageRequest.of((from) % size, size, Sort.by("eventDateTime")
                            .descending());
                } else {
                    paging = PageRequest.of(from, size);
                }
            } else {
                throw new ConflictException("Неверная сортировка. Используй VIEW or EVENT_DATE");
            }
        }
        if (onlyAvailable) {
            if (rangeStart == null || rangeEnd == null) {
                events = eventRepository.findAllAvailablePublishedEventsByCategoryAndStateAfterDate(text,
                        now().toInstant(ZoneOffset.UTC), categories, paging, EventState.PUBLISHED, paid);
            } else {
                events = eventRepository.findAllAvailablePublishedEventsByCategoryAndStateBetweenDates(text,
                        rangeStart.toInstant(ZoneOffset.UTC), rangeEnd.toInstant(ZoneOffset.UTC), categories, paging,
                        EventState.PUBLISHED, paid);
            }
        } else {
            if (rangeStart == null || rangeEnd == null) {
                events = eventRepository.findAllEventsWithStatusAfterDate(text, now().toInstant(ZoneOffset.UTC),
                        categories, EventState.PUBLISHED, paging, paid);
            } else {
                events = eventRepository.findAllEventsWithStatusBetweenDates(text,
                        rangeStart.toInstant(ZoneOffset.UTC), rangeEnd.toInstant(ZoneOffset.UTC), categories,
                        EventState.PUBLISHED, paging, paid);
            }
        }

        return getEventsShorts(events.stream().toList());
    }

    public List<EventShortDto> getByUserId(Long userId, Pageable paging) {
        userClient.getUserShortById(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, paging).stream().toList();

        return getEventsShorts(events);
    }

    public EventDto getEventByUserId(Long userId, Long eventId) {
        Event event = getEventIfExist(eventId);
        userClient.getUserShortById(userId);
        if (!Objects.equals(event.getInitiatorId(), userId)) {
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
        Long confirmedRequests = requestClient.getConfirmedRequestsCount(event.getId(), RequestStatus.CONFIRMED);
        Integer views = getEventsViews(event.getId());

        return EventMapper.fromEventToEventDto(
                event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                userClient.getUserShortById(event.getInitiatorId()),confirmedRequests,
                views
        );
    }

    private List<HashMap<Object, Object>> getStats(List<String> uris) {
        return (List<HashMap<Object, Object>>) statsClient.getStats("2000-01-01 00:00:00",
                now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                uris, false).getBody();
    }

    private List<EventDto> getEventsFulls(List<Event> events) {
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Integer> viewsMap = getEventsViewsMap(eventIds);

        return events.stream()
                .map(event -> {
                    Long confirmed =
                            requestClient.getConfirmedRequestsCount(event.getId(), RequestStatus.CONFIRMED);

                    return EventMapper.fromEventToEventDto(
                            event,
                            EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                            userClient.getUserShortById(event.getInitiatorId()),
                            confirmed,
                            viewsMap.getOrDefault(event.getId(), 0)
                    );
                })
                .toList();
    }

    private List<EventShortDto> getEventsShorts(List<Event> events) {

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Integer> viewsMap = getEventsViewsMap(eventIds);

        return events.stream()
                .map(event -> {
                    Long confirmed =
                            requestClient.getConfirmedRequestsCount(event.getId(), RequestStatus.CONFIRMED);

                    return EventMapper.fromEventToEventShortDto(
                            event,
                            EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                            userClient.getUserShortById(event.getInitiatorId()),
                            confirmed,
                            viewsMap.getOrDefault(event.getId(), 0)
                    );
                })
                .toList();
    }
}