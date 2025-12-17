package ru.yandex.practicum.main.event.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.exception.model.ServiceUnavailableException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.StatsClient;
import ru.yandex.practicum.client.request.RequestClient;
import ru.yandex.practicum.client.user.UserClient;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.enums.AdminEventAction;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.enums.UserEventActions;
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
import feign.FeignException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static java.time.LocalDateTime.now;

@Service
@Slf4j
public class EventService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final RequestClient requestClient;
    private final UserClient userClient;
    private final StatsClient statsClient;

    @Autowired
    public EventService(EventCategoryRepository categoryRepository,
                        EventRepository eventRepository,
                        LocationRepository locationRepository,
                        RequestClient requestClient,
                        UserClient userClient,
                        StatsClient statsClient) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.locationRepository = locationRepository;
        this.requestClient = requestClient;
        this.userClient = userClient;
        this.statsClient = statsClient;
    }

    public EventDto create(NewEventDto dto, Long userId) {
        checkUserExists(userId);

        EventCategory category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не существует"));

        validateEventDate(dto.getEventDate(), false);

        Event event = EventMapper.fromCreateNewEventDtoToEvent(dto, category, userId);
        event.setInitiatorId(userId);
        event.setLocation(saveLocation(event.getLocation() != null ? event.getLocation() : new Location(null, 0.0, 0.0)));

        Event saved = eventRepository.save(event);

        var initiator = userClient.getUser(userId);
        return EventMapper.fromEventToEventDto(
                saved,
                EventCategoryMapper.toCategoryDtoFromCategory(category),
                initiator,
                0L,
                0
        );
    }

    public EventDto updateByAdmin(Long eventId, UpdateEventAdminDto dto) {
        Event event = getEventIfExist(eventId);

        if (!event.getState().equals(EventState.PENDING)) {
            throw new ConflictException("Изменять можно только событие в состоянии PENDING");
        }

        validateEventDate(dto.getEventDate(), true);

        updateEventFields(event, dto);

        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == AdminEventAction.PUBLISH_EVENT) {
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(now().toInstant(ZoneOffset.UTC));
            } else if (dto.getStateAction() == AdminEventAction.REJECT_EVENT) {
                event.setState(EventState.CANCELED);
            }
        }

        event.setLocation(saveLocation(event.getLocation()));
        Event updated = eventRepository.save(event);

        return getEventDtoFromEvent(updated);
    }

    public List<EventDto> getAll(List<Long> users, List<String> states, List<Long> categories,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        if (size <= 0) {
            throw new BadRequestException("Параметр size должен быть положительным");
        }
        Pageable pageable = PageRequest.of(from / size, size);
        List<EventState> eventStates = states == null ? null :
                states.stream().map(EventState::valueOf).toList();

        List<Long> categoryIds = (categories == null || categories.isEmpty())
                ? categoryRepository.findAll().stream().map(EventCategory::getId).toList()
                : categories;

        Instant start = now().toInstant(ZoneOffset.UTC);
        Instant end = null;
        if (rangeStart != null && rangeEnd != null) {
            start = rangeStart.toInstant(ZoneOffset.UTC);
            end = rangeEnd.toInstant(ZoneOffset.UTC);
        }

        Page<Event> page = (end == null)
                ? eventRepository.findAllEventsAfterDateForUsersByStateAndCategories(users, eventStates, categoryIds, start, pageable)
                : eventRepository.findAllEventsBetweenDatesForUsersByStateAndCategories(users, eventStates, categoryIds, start, end, pageable);

        return getEventsFulls(page.getContent());
    }

    public EventDto getById(Long eventId) {
        Event event = getEventIfExist(eventId);
        if (event.getPublishedOn() == null) {
            throw new NotFoundException("Событие с id=" + eventId + " ещё не опубликовано");
        }

        return getEventDtoFromEvent(event);
    }

    public EventDto updateByUser(UpdateEventUserRequest dto, Long userId, Long eventId) {
        Event event = getEventIfExist(eventId);
        checkUserExists(userId);

        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new NotFoundException("Пользователь не является инициатором события");
        }

        if (!List.of(EventState.PENDING, EventState.CANCELED).contains(event.getState())) {
            throw new ConflictException("Редактировать можно только события в состоянии PENDING или CANCELED");
        }

        validateEventDate(dto.getEventDate(), false);

        updateEventFields(event, dto);

        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == UserEventActions.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            } else if (dto.getStateAction() == UserEventActions.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            }
        }

        return getEventDtoFromEvent(eventRepository.save(event));
    }

    public List<EventShortDto> getAllShort(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           boolean onlyAvailable, String sort, int from, int size) {

        Sort dbSort = "EVENT_DATE".equalsIgnoreCase(sort) ? Sort.by("eventDateTime").descending() : Sort.unsorted();
        Pageable pageable = "VIEWS".equalsIgnoreCase(sort) ? Pageable.unpaged() : PageRequest.of(from / size, size, dbSort);
        Page<Event> page = getPublicEventsPage(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable);
        List<EventShortDto> dtos = getEventsShorts(page.getContent());

        if ("VIEWS".equalsIgnoreCase(sort)) {
            dtos = dtos.stream()
                    .sorted(Comparator.comparing(EventShortDto::getViews).reversed())
                    .skip(from)
                    .limit(size)
                    .toList();
        }

        return dtos;
    }

    private Page<Event> getPublicEventsPage(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            boolean onlyAvailable, Pageable pageable) {
        Instant start = (rangeStart != null ? rangeStart : now()).toInstant(ZoneOffset.UTC);
        Instant end = rangeEnd != null ? rangeEnd.toInstant(ZoneOffset.UTC) : null;

        if (onlyAvailable) {
            return end == null
                    ? eventRepository.findAllAvailablePublishedEventsByCategoryAndStateAfterDate(text, start, categories, paid, pageable)
                    : eventRepository.findAllAvailablePublishedEventsByCategoryAndStateBetweenDates(text, start, end, categories,paid, pageable);
        } else {
            return end == null
                    ? eventRepository.findAllEventsWithStatusAfterDate(text, start, categories, paid, pageable)
                    : eventRepository.findAllEventsWithStatusBetweenDates(text, start, end, categories, paid, pageable);
        }
    }

    public List<EventShortDto> getByUserId(Long userId, Pageable pageable) {
        checkUserExists(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).getContent();
        return getEventsShorts(events);
    }

    public EventDto getEventByUserId(Long userId, Long eventId) {
        Event event = getEventIfExist(eventId);
        checkUserExists(userId);

        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new NotFoundException("Пользователь не является инициатором события");
        }

        return getEventDtoFromEvent(event);
    }

    public Event getEventIfExist(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не существует!"));
    }

    private void checkUserExists(Long userId) {
        try {
            var user = userClient.getUser(userId);
            if (user == null) {
                throw new ServiceUnavailableException("User service is unavailable");
            }
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private void validateEventDate(LocalDateTime eventDate, boolean isAdmin) {
        if (eventDate == null) return;

        long hours = isAdmin ? 1 : 2;
        if (eventDate.isBefore(now().plusHours(hours))) {
            throw new BadRequestException(
                    "Дата и время события должны быть не ранее чем через " + hours + " час(а) от текущего момента"
            );
        }
    }

    private void updateEventFields(Event event, Object dto) {
        if (dto instanceof UpdateEventAdminDto adminDto) {
            if (adminDto.getCategory() != null) {
                event.setCategory(getCategoryOrThrow(adminDto.getCategory()));
            }
            if (adminDto.getAnnotation() != null) event.setAnnotation(adminDto.getAnnotation());
            if (adminDto.getDescription() != null) event.setDescription(adminDto.getDescription());
            if (adminDto.getTitle() != null) event.setTitle(adminDto.getTitle());
            updateCommonFields(event, adminDto.getEventDate(), adminDto.getLocation(), adminDto.getPaid(),
                    adminDto.getParticipantLimit(), adminDto.getRequestModeration());
        } else if (dto instanceof UpdateEventUserRequest userDto) {
            if (userDto.getCategory() != null) {
                event.setCategory(getCategoryOrThrow(userDto.getCategory()));
            }
            if (userDto.getAnnotation() != null && !userDto.getAnnotation().isBlank()) event.setAnnotation(userDto.getAnnotation());
            if (userDto.getDescription() != null && !userDto.getDescription().isBlank()) event.setDescription(userDto.getDescription());
            if (userDto.getTitle() != null && !userDto.getTitle().isBlank()) event.setTitle(userDto.getTitle());
            updateCommonFields(event, userDto.getEventDate(), userDto.getLocation(), userDto.getPaid(),
                    userDto.getParticipantLimit(), userDto.getRequestModeration());
        }
    }

    private EventCategory getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не существует"));
    }

    private void updateCommonFields(Event event, LocalDateTime eventDate, Object locationObj, Boolean paid,
                                    Long participantLimit, Boolean requestModeration) {
        if (eventDate != null) event.setEventDateTime(eventDate.toInstant(ZoneOffset.UTC));
        if (locationObj != null) {
            Location location = locationObj instanceof LocationDto l ? LocationMapper.location(l) : (Location) locationObj;
            event.setLocation(saveLocation(location));
        }
        if (paid != null) event.setPaid(paid);
        if (participantLimit != null) event.setParticipantLimit(participantLimit);
        if (requestModeration != null) event.setIsModerated(requestModeration);
    }

    private Location saveLocation(Location location) {
        return locationRepository.findByLatAndLon(location.getLat(), location.getLon())
                .orElseGet(() -> locationRepository.save(location));
    }

    private Map<Long, Integer> getEventsViewsMap(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) return Map.of();

        List<String> uris = eventIds.stream().map(id -> "/events/" + id).toList();
        List<HashMap<Object, Object>> stats = getStats(uris);

        Map<Long, Integer> views = new HashMap<>();
        if (stats != null) {
            for (var map : stats) {
                String uri = (String) map.get("uri");
                if (uri != null && uri.startsWith("/events/")) {
                    String idStr = uri.substring(uri.lastIndexOf('/') + 1);
                    try {
                        Long id = Long.parseLong(idStr);
                        views.put(id, (Integer) map.getOrDefault("hits", 0));
                    } catch (NumberFormatException e) {
                        log.warn("Некорректный URI от stats-service: {}", uri);
                    }
                }
            }
        }
        eventIds.forEach(id -> views.putIfAbsent(id, 0));
        return views;
    }

    private List<HashMap<Object, Object>> getStats(List<String> uris) {
        try {
            var response = statsClient.getStats(
                    "2000-01-01 00:00:00",
                    now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    uris,
                    false
            );

            if (response != null && response.getBody() instanceof List<?> body) {
                @SuppressWarnings("unchecked")
                List<HashMap<Object, Object>> stats = (List<HashMap<Object, Object>>) body;
                return stats;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Ошибка при запросе статистики просмотров: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private EventDto getEventDtoFromEvent(Event event) {
        Long confirmed;
        try {
            confirmed = requestClient.countByStatus(event.getId(), RequestStatus.CONFIRMED);
        } catch (FeignException e) {
            log.warn("Ошибка при запросе подтверждённых заявок для события {}: {}", event.getId(), e.getMessage());
            confirmed = 0L;
        }
        Integer views = getEventsViewsMap(List.of(event.getId())).getOrDefault(event.getId(), 0);

        var initiator = userClient.getUser(event.getInitiatorId());
        if (initiator == null) {
            throw new ServiceUnavailableException("User service is unavailable");
        }

        return EventMapper.fromEventToEventDto(
                event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                initiator,
                confirmed,
                views
        );
    }

    private List<EventDto> getEventsFulls(List<Event> events) {
        Map<Long, Integer> viewsMap = getEventsViewsMap(events.stream().map(Event::getId).toList());
        Map<Long, Long> confirmedMap = requestClient.countByStatusBatch(
                events.stream().map(Event::getId).toList(),
                RequestStatus.CONFIRMED
        );
        Map<Long, UserShortDto> usersMap = userClient.getUsersBatch(
                events.stream().map(Event::getInitiatorId).distinct().toList()
        );

        return events.stream()
                .map(e -> {
                    Long confirmed = confirmedMap.getOrDefault(e.getId(), 0L);
                    return EventMapper.fromEventToEventDto(
                            e,
                            EventCategoryMapper.toCategoryDtoFromCategory(e.getCategory()),
                            usersMap.get(e.getInitiatorId()),
                            confirmed,
                            viewsMap.getOrDefault(e.getId(), 0)
                    );
                })
                .toList();
    }

    private List<EventShortDto> getEventsShorts(List<Event> events) {
        Map<Long, Integer> viewsMap = getEventsViewsMap(events.stream().map(Event::getId).toList());

        return events.stream()
                .map(e -> {
                    Long confirmed = requestClient.countByStatus(e.getId(), RequestStatus.CONFIRMED);
                    return EventMapper.fromEventToEventShortDto(
                            e,
                            EventCategoryMapper.toCategoryDtoFromCategory(e.getCategory()),
                            userClient.getUser(e.getInitiatorId()),
                            confirmed,
                            viewsMap.getOrDefault(e.getId(), 0)
                    );
                })
                .toList();
    }
}