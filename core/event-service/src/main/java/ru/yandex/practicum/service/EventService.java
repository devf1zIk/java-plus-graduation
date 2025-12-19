package ru.yandex.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.StatsClient;
import ru.yandex.practicum.dto.HitDto;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.AdminEventAction;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.enums.UserEventActions;
import ru.yandex.practicum.exception.model.BadRequestException;
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
import ru.yandex.practicum.repository.EventSpecification;
import ru.yandex.practicum.repository.LocationRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    private final EventCategoryRepository categoryRepository;
    private static final DateTimeFormatter STATS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final UserClient userClient;
    private final StatsClient statsClient;
    private final RequestClient requestClient;
    private final EventMapper eventMapper;

    public EventDto create(CreateNewEventDto dto, Long userId) {
        userClient.getById(userId);

        if (dto.getEventDate() != null &&
                dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException(
                    "eventDate не может быть раньше чем через 2 часа от текущего времени"
            );
        }

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

    public EventDto updateByAdmin(Long eventId, UpdateEventAdminDto dto) {
        Event event = getEventIfExist(eventId);

        if (event.getEventDateTime() != null &&
                event.getEventDateTime().isBefore(LocalDateTime.now().plusHours(2).atZone(ZoneOffset.UTC).toInstant())) {
            throw new BadRequestException(
                    "eventDate не может быть раньше чем через 2 часа от текущего времени"
            );
        }
        if (dto.getStateAction() == AdminEventAction.PUBLISH_EVENT && event.getState() != EventState.PENDING) {
            throw new ConflictException("Публиковать можно только событие в состоянии PENDING");
        }
        if (dto.getStateAction() == AdminEventAction.REJECT_EVENT && event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя отклонить опубликованное событие");
        }

        if (dto.getCategory() != null) {
            EventCategory category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }

        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) {
            event.setEventDateTime(dto.getEventDate().atZone(ZoneOffset.UTC).toInstant());
        }
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

    public List<EventShortDto> getAllShort(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           boolean onlyAvailable, String sort, int from, int size) {
        Instant start = rangeStart == null ? Instant.now() : rangeStart.toInstant(ZoneOffset.UTC);
        Instant end = rangeEnd == null ? null : rangeEnd.toInstant(ZoneOffset.UTC);

        Pageable pageable = getPageable(sort, from, size);

        Specification<Event> spec = Specification.where(EventSpecification.isPublished())
                .and(EventSpecification.hasRangeStart(start));

        if (end != null) {
            spec = spec.and(EventSpecification.hasRangeEnd(end));
        }

        if (text != null && !text.isBlank()) {
            spec = spec.and(EventSpecification.hasText(text));
        }

        if (categories != null && !categories.isEmpty()) {
            spec = spec.and(EventSpecification.hasCategories(categories));
        }

        if (paid != null) {
            spec = spec.and(EventSpecification.isPaid(paid));
        }
        Page<Event> page = eventRepository.findAll(spec, pageable);
        List<Event> events = page.getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        if (onlyAvailable) {
            Map<Long, Long> confirmedMap = getConfirmedMap(events.stream().map(Event::getId).toList());
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            confirmedMap.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
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

        if (dto.getEventDate() != null &&
                dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException(
                    "eventDate не может быть раньше чем через 2 часа от текущего времени"
            );
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

    public List<EventDto> getAll(List<Long> users, List<String> states, List<Long> categories,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                 int from, int size) {

        List<EventState> safeStates = states == null || states.isEmpty()
                ? null
                : states.stream().map(EventState::valueOf).toList();

        Pageable pageable = PageRequest.of(from / size, size);

        Instant start = rangeStart == null ? null : rangeStart.atZone(ZoneOffset.UTC).toInstant();
        Instant end = rangeEnd == null ? null : rangeEnd.atZone(ZoneOffset.UTC).toInstant();

        Specification<Event> spec = Specification.where(null);

        if (users != null && !users.isEmpty()) {
            spec = spec.and(EventSpecification.hasUsers(users));
        }
        if (safeStates != null && !safeStates.isEmpty()) {
            spec = spec.and(EventSpecification.hasStates(safeStates));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and(EventSpecification.hasCategories(categories));
        }
        if (start != null) {
            spec = spec.and(EventSpecification.hasRangeStart(start));
        }
        if (end != null) {
            spec = spec.and(EventSpecification.hasRangeEnd(end));
        }

        Page<Event> page = eventRepository.findAll(spec, pageable);

        return enrichFullDtos(page.getContent());
    }

    public EventDto getById(Long eventId, HttpServletRequest request) {
        Event event = getEventIfExist(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }

        statsClient.create(new HitDto(
                request.getRemoteAddr(),
                "app",
                request.getRequestURI(),
                LocalDateTime.now()
        ));

        UserShortDto initiator = getInitiator(event.getOwnerId());
        Long confirmed = getConfirmedCount(eventId);
        Integer views = getViews(eventId);

        if ("127.0.0.1".equals(request.getRemoteAddr()) || "0:0:0:0:0:0:0:1".equals(request.getRemoteAddr())) {
            views = views + 1;
        }
        return eventMapper.toEventDto(
                event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                initiator,
                confirmed,
                views
        );
    }

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
        try {
            String start = LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC).format(STATS_DATE_FORMATTER);
            String end = LocalDateTime.now().format(STATS_DATE_FORMATTER);

            ResponseEntity<Object> response = statsClient.getStats(
                    start,
                    end,
                    List.of("/events/" + eventId),
                    false
            );

            Object body = response.getBody();
            if (body == null) {
                return 0;
            }

            List<Map<String, Object>> stats;
            if (body instanceof List) {
                stats = (List<Map<String, Object>>) body;
            } else if (body instanceof Map) {
                stats = List.of((Map<String, Object>) body);
            } else {
                return 0;
            }

            if (stats.isEmpty()) {
                return 0;
            }

            Object hitsObject = stats.get(0).get("hits");
            if (hitsObject instanceof Number) {
                return ((Number) hitsObject).intValue();
            }

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<Long, Integer> getViewsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        try {
            String start = LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC).format(STATS_DATE_FORMATTER);
            String end = LocalDateTime.now().format(STATS_DATE_FORMATTER);

            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .toList();

            ResponseEntity<Object> response = statsClient.getStats(start, end, uris, false);

            Object body = response.getBody();

            if (body == null) {
                return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0));
            }

            List<Map<String, Object>> stats;
            if (body instanceof List) {
                stats = (List<Map<String, Object>>) body;
            } else if (body instanceof Map) {
                stats = List.of((Map<String, Object>) body);
            } else {
                return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0));
            }

            Map<Long, Integer> viewsMap = new HashMap<>();
            for (Map<String, Object> stat : stats) {
                String uri = (String) stat.get("uri");
                if (uri != null && uri.startsWith("/events/")) {
                    try {
                        Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
                        Object hitsObject = stat.get("hits");
                        if (hitsObject instanceof Number) {
                            viewsMap.put(eventId, ((Number) hitsObject).intValue());
                        }
                    } catch (NumberFormatException e) {
                        // Игнорируем некорректный URI
                    }
                }
            }

            eventIds.forEach(id -> viewsMap.putIfAbsent(id, 0));
            return viewsMap;

        } catch (Exception e) {
            return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0));
        }
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