package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.StatsClient;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.AdminEventAction;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.enums.UserEventActions;
import ru.yandex.practicum.exception.model.ConflictException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.feign.request.RequestClient;
import ru.yandex.practicum.feign.user.UserClient;
import ru.yandex.practicum.mapper.EventCategoryMapper;
import ru.yandex.practicum.mapper.EventMapper;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.EventCategory;
import ru.yandex.practicum.model.Location;
import ru.yandex.practicum.repository.EventCategoryRepository;
import ru.yandex.practicum.repository.EventRepository;
import ru.yandex.practicum.repository.LocationRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final StatsClient statsClient;
    private final EventMapper eventMapper;

    // CREATE
    public EventDto create(CreateNewEventDto dto, Long userId) {
        userClient.getById(userId);

        EventCategory category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));

        Event event = eventMapper.toEvent(
                dto,
                userId,
                category
        );

        event.setLocation(saveLocation(dto.getLocation()));

        Event saved = eventRepository.save(event);

        return eventMapper.toEventDto(
                saved,
                EventCategoryMapper.toCategoryDtoFromCategory(category),
                getInitiator(userId),
                0L,
                0
        );
    }

    // ADMIN UPDATE
    public EventDto updateByAdmin(Long eventId, UpdateEventAdminDto dto) {
        Event event = getEventIfExist(eventId);

        if (dto.getStateAction() == AdminEventAction.PUBLISH_EVENT && event.getState() != EventState.PENDING) {
            throw new ConflictException("Публиковать можно только событие в состоянии PENDING");
        }

        if (dto.getCategory() != null) {
            EventCategory category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }

        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDateTime(dto.getEventDate().toInstant(ZoneOffset.UTC));
        if (dto.getLocation() != null) event.setLocation(saveLocation(dto.getLocation()));
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setIsModerated(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());

        if (dto.getStateAction() == AdminEventAction.PUBLISH_EVENT) {
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(Instant.now());
        } else if (dto.getStateAction() == AdminEventAction.REJECT_EVENT) {
            event.setState(EventState.CANCELED);
        }

        Event saved = eventRepository.save(event);

        UserShortDto initiator = getInitiator(saved.getOwnerId());
        Long confirmed = getConfirmedCount(saved.getId());
        Integer views = getViews(saved.getId());

        return eventMapper.toEventDto(
                saved,
                EventCategoryMapper.toCategoryDtoFromCategory(saved.getCategory()),
                initiator,
                confirmed,
                views
        );
    }

    // PUBLIC SEARCH
    public List<EventShortDto> getAllShort(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           boolean onlyAvailable, String sort, int from, int size) {
        Instant start = rangeStart == null ? Instant.now() : rangeStart.toInstant(ZoneOffset.UTC);
        Instant end = rangeEnd == null ? null : rangeEnd.toInstant(ZoneOffset.UTC);

        Pageable pageable = getPageable(sort, from, size);

        Page<Event> page = end == null
                ? eventRepository.findPublishedAfterDate(text, categories, paid, start, pageable)
                : eventRepository.findPublishedInRange(text, categories, paid, start, end, pageable);

        List<Event> events = page.getContent();
        if (events.isEmpty()) {
            return List.of();
        }

        // Фильтр onlyAvailable на уровне Event
        if (onlyAvailable) {
            Map<Long, Long> confirmedMap = getConfirmedMap(events.stream().map(Event::getId).toList());
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 || confirmedMap.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                    .toList();
        }

        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<Long> ownerIds = events.stream().map(Event::getOwnerId).distinct().toList();

        Map<Long, Long> confirmedMap = getConfirmedMap(eventIds);
        Map<Long, UserShortDto> initiatorMap = getInitiatorsMap(ownerIds);
        Map<Long, Integer> viewsMap = getViewsMap(eventIds);

        return events.stream()
                .map(event -> eventMapper.toEventShortDto(
                        event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        initiatorMap.getOrDefault(event.getOwnerId(), new UserShortDto(event.getOwnerId(), "Unknown User")),
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0)
                ))
                .toList();
    }

    // PRIVATE
    public List<EventShortDto> getByUserId(Long userId, Pageable pageable) {
        userClient.getById(userId);
        Page<Event> page = eventRepository.findAllByOwnerId(userId, pageable);
        return enrichShortDtos(page.getContent());
    }

    public EventDto getEventByUserId(Long userId, Long eventId) {
        userClient.getById(userId);
        Event event = getEventIfExist(eventId);
        if (!event.getOwnerId().equals(userId)) {
            throw new NotFoundException("Пользователь не является инициатором события");
        }

        UserShortDto initiator = getInitiator(userId);
        Long confirmed = getConfirmedCount(eventId);
        Integer views = getViews(eventId);

        return eventMapper.toEventDto(
                event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                initiator,
                confirmed,
                views
        );
    }

    public EventDto updateByUser(UpdateEventUserRequest dto, Long userId, Long eventId) {
        userClient.getById(userId);
        Event event = getEventIfExist(eventId);
        if (!event.getOwnerId().equals(userId)) {
            throw new NotFoundException("Пользователь не является инициатором");
        }
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Редактировать можно только в PENDING или CANCELED");
        }

        if (dto.getCategory() != null) {
            EventCategory category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDateTime(dto.getEventDate().toInstant(ZoneOffset.UTC));
        if (dto.getLocation() != null) event.setLocation(saveLocation(dto.getLocation()));
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setIsModerated(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());

        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == UserEventActions.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            } else if (dto.getStateAction() == UserEventActions.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            }
        }

        Event saved = eventRepository.save(event);

        UserShortDto initiator = getInitiator(userId);
        Long confirmed = getConfirmedCount(eventId);
        Integer views = getViews(eventId);

        return eventMapper.toEventDto(
                saved,
                EventCategoryMapper.toCategoryDtoFromCategory(saved.getCategory()),
                initiator,
                confirmed,
                views
        );
    }

    // ADMIN SEARCH
    public List<EventDto> getAll(List<Long> users, List<String> states, List<Long> categories,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        List<EventState> eventStates = states == null ? null : states.stream().map(EventState::valueOf).toList();
        Instant start = rangeStart == null ? Instant.now() : rangeStart.toInstant(ZoneOffset.UTC);
        Instant end = rangeEnd == null ? null : rangeEnd.toInstant(ZoneOffset.UTC);

        Pageable pageable = PageRequest.of(from / size, size);

        Page<Event> page = end == null
                ? eventRepository.findForAdminAfterDate(users, eventStates, categories, start, pageable)
                : eventRepository.findForAdminInRange(users, eventStates, categories, start, end, pageable);

        return enrichFullDtos(page.getContent());
    }

    // PUBLIC SINGLE
    public EventDto getById(Long eventId) {
        Event event = getEventIfExist(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }

        UserShortDto initiator = getInitiator(event.getOwnerId());
        Long confirmed = getConfirmedCount(eventId);
        Integer views = getViews(eventId) + 1; // +1 за текущий просмотр

        return eventMapper.toEventDto(
                event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                initiator,
                confirmed,
                views
        );
    }

    // ВСПОМОГАТЕЛЬНЫЕ
    public Event getEventIfExist(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private Location saveLocation(LocationDto dto) {
        if (dto == null) return null;
        return locationRepository.findByLatAndLon(dto.getLat(), dto.getLon())
                .orElseGet(() -> locationRepository.save(new Location(null, dto.getLat(), dto.getLon())));
    }

    private UserShortDto getInitiator(Long userId) {
        try {
            return userClient.getById(userId);
        } catch (Exception e) {
            return new UserShortDto(userId, "Unknown User");
        }
    }

    private Map<Long, UserShortDto> getInitiatorsMap(List<Long> ownerIds) {
        if (ownerIds.isEmpty()) return Map.of();
        try {
            List<UserShortDto> users = userClient.getByIds(ownerIds);
            return users.stream().collect(Collectors.toMap(UserShortDto::getId, u -> u));
        } catch (Exception e) {
            return ownerIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> new UserShortDto(id, "Unknown User")));
        }
    }

    private Long getConfirmedCount(Long eventId) {
        try {
            Map<Long, Long> map = requestClient.getConfirmedCounts(List.of(eventId));
            return map.getOrDefault(eventId, 0L);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Map<Long, Long> getConfirmedMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        try {
            return requestClient.getConfirmedCounts(eventIds);
        } catch (Exception e) {
            return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0L));
        }
    }

    private Integer getViews(Long eventId) {
        return 0; // заглушка — подключи свой StatsClient
    }

    private Map<Long, Integer> getViewsMap(List<Long> eventIds) {
        return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0)); // заглушка
    }

    private Pageable getPageable(String sort, int from, int size) {
        int page = from / size;
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }
        if (sort.equals("EVENT_DATE")) {
            return PageRequest.of(page, size, Sort.by("eventDateTime").descending());
        }
        return PageRequest.of(page, size);
    }

    private List<EventShortDto> enrichShortDtos(List<Event> events) {
        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<Long> ownerIds = events.stream().map(Event::getOwnerId).distinct().toList();

        Map<Long, Long> confirmedMap = getConfirmedMap(eventIds);
        Map<Long, UserShortDto> initiatorMap = getInitiatorsMap(ownerIds);
        Map<Long, Integer> viewsMap = getViewsMap(eventIds);

        return events.stream()
                .map(event -> eventMapper.toEventShortDto(
                        event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        initiatorMap.getOrDefault(event.getOwnerId(), new UserShortDto(event.getOwnerId(), "Unknown User")),
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0)
                ))
                .toList();
    }

    private List<EventDto> enrichFullDtos(List<Event> events) {
        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<Long> ownerIds = events.stream().map(Event::getOwnerId).distinct().toList();

        Map<Long, Long> confirmedMap = getConfirmedMap(eventIds);
        Map<Long, UserShortDto> initiatorMap = getInitiatorsMap(ownerIds);
        Map<Long, Integer> viewsMap = getViewsMap(eventIds);

        return events.stream()
                .map(event -> eventMapper.toEventDto(
                        event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        initiatorMap.getOrDefault(event.getOwnerId(), new UserShortDto(event.getOwnerId(), "Unknown User")),
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0)
                ))
                .toList();
    }
}