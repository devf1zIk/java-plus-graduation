package ru.yandex.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.StatsClient;
import ru.yandex.practicum.dto.HitDto;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.*;
import ru.yandex.practicum.exception.model.*;
import ru.yandex.practicum.feign.request.RequestClient;
import ru.yandex.practicum.feign.user.UserClient;
import ru.yandex.practicum.mapper.EventCategoryMapper;
import ru.yandex.practicum.mapper.EventMapper;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.EventCategory;
import ru.yandex.practicum.model.Location;
import ru.yandex.practicum.repository.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    private final EventCategoryRepository categoryRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final UserClient userClient;
    private final StatsClient statsClient;
    private final RequestClient requestClient;
    private final EventMapper eventMapper;

    public EventDto create(CreateNewEventDto dto, Long userId) {
        userClient.getById(userId);

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }

        EventCategory category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + dto.getCategory() + " не найдена"));

        Event event = eventMapper.toEvent(dto, userId, category);
        event.setLocation(saveLocation(dto.getLocation()));
        event.setState(EventState.PENDING);
        event.setCreatedOn(Instant.now());

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

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }

        if (dto.getStateAction() == AdminEventAction.PUBLISH_EVENT && event.getState() != EventState.PENDING) {
            throw new ConflictException("Опубликовать можно только событие в состоянии ожидания модерации");
        }
        if (dto.getStateAction() == AdminEventAction.REJECT_EVENT && event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя отклонить уже опубликованное событие");
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

        return enrichEventDto(saved);
    }

    public List<EventShortDto> getAllShort(String text,
                                           List<Long> categories,
                                           Boolean paid,
                                           LocalDateTime rangeStart,
                                           LocalDateTime rangeEnd,
                                           boolean onlyAvailable,
                                           String sort,
                                           int from,
                                           int size) {

        Instant start = rangeStart == null ? Instant.now() : rangeStart.toInstant(ZoneOffset.UTC);
        Instant end = rangeEnd == null ? null : rangeEnd.toInstant(ZoneOffset.UTC);

        Pageable pageable = getPageable(sort, from, size);

        Specification<Event> spec = EventSpecification.isPublished()
                .and(EventSpecification.hasRangeStart(start));

        if (end != null) spec = spec.and(EventSpecification.hasRangeEnd(end));
        if (text != null && !text.isBlank()) spec = spec.and(EventSpecification.hasText(text));
        if (categories != null && !categories.isEmpty()) spec = spec.and(EventSpecification.hasCategories(categories));
        if (paid != null) spec = spec.and(EventSpecification.isPaid(paid));

        Page<Event> page = eventRepository.findAll(spec, pageable);
        List<Event> events = page.getContent();

        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<Long> ownerIds = events.stream().map(Event::getOwnerId).distinct().collect(Collectors.toList());

        Map<Long, Long> confirmedMap = getConfirmedMap(eventIds);
        Map<Long, UserShortDto> initiatorMap = getInitiatorsMap(ownerIds);
        Map<Long, Integer> viewsMap = getViewsMap(eventIds);

        if (onlyAvailable) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            confirmedMap.getOrDefault(e.getId(), 0L) < e.getParticipantLimit())
                    .collect(Collectors.toList());
        }

        if (events.isEmpty()) {
            return List.of();
        }

        return events.stream()
                .map(event -> eventMapper.toEventShortDto(
                        event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        initiatorMap.getOrDefault(event.getOwnerId(), new UserShortDto(event.getOwnerId(), "Unknown")),
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0)
                ))
                .collect(Collectors.toList());
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
            throw new NotFoundException("Событие не принадлежит пользователю");
        }
        return enrichEventDto(event);
    }

    public EventDto updateByUser(UpdateEventUserRequest dto, Long userId, Long eventId) {
        userClient.getById(userId);
        Event event = getEventIfExist(eventId);

        if (!event.getOwnerId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Изменить можно только событие в состоянии ожидания или отменённое");
        }

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не раньше чем через 2 часа");
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

        if (dto.getStateAction() == UserEventActions.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (dto.getStateAction() == UserEventActions.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }

        Event saved = eventRepository.save(event);
        return enrichEventDto(saved);
    }

    public List<EventDto> getAll(List<Long> users, List<String> states, List<Long> categories,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                 int from, int size) {

        List<EventState> eventStates = states == null || states.isEmpty()
                ? null
                : states.stream().map(EventState::valueOf).collect(Collectors.toList());

        Pageable pageable = PageRequest.of(from / size, size);

        Instant start = rangeStart == null ? null : rangeStart.toInstant(ZoneOffset.UTC);
        Instant end = rangeEnd == null ? null : rangeEnd.toInstant(ZoneOffset.UTC);

        Specification<Event> spec = Specification.where(null);

        if (users != null && !users.isEmpty()) spec = spec.and(EventSpecification.hasUsers(users));
        if (eventStates != null && !eventStates.isEmpty()) spec = spec.and(EventSpecification.hasStates(eventStates));
        if (categories != null && !categories.isEmpty()) spec = spec.and(EventSpecification.hasCategories(categories));
        if (start != null) spec = spec.and(EventSpecification.hasRangeStart(start));
        if (end != null) spec = spec.and(EventSpecification.hasRangeEnd(end));

        Page<Event> page = eventRepository.findAll(spec, pageable);
        return enrichFullDtos(page.getContent());
    }

    public EventDto getById(Long eventId, HttpServletRequest request) {
        Event event = getEventIfExist(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие должно быть опубликовано");
        }

        statsClient.create(HitDto.builder()
                .ip(request.getRemoteAddr())
                .app("app")
                .uri(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build());

        EventDto dto = enrichEventDto(event);

        if ("127.0.0.1".equals(request.getRemoteAddr()) || "0:0:0:0:0:0:0:1".equals(request.getRemoteAddr())) {
            dto.setViews(dto.getViews() + 1);
        }

        return dto;
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
            return ownerIds.stream().collect(Collectors.toMap(id -> id, id -> new UserShortDto(id, "Unknown User")));
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

    private Map<Long, Integer> getViewsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        String start = LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC).format(FORMATTER);
        String end = LocalDateTime.now(ZoneOffset.UTC).format(FORMATTER);
        List<String> uris = eventIds.stream().map(id -> "/events/" + id).collect(Collectors.toList());

        try {
            ResponseEntity<Object> response = statsClient.getStats(start, end, uris, false);
            Object body = response.getBody();

            if (body == null) {
                return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0));
            }

            List<Map<String, Object>> stats = body instanceof List
                    ? (List<Map<String, Object>>) body
                    : List.of((Map<String, Object>) body);

            Map<Long, Integer> views = new HashMap<>();
            for (Map<String, Object> stat : stats) {
                String uri = (String) stat.get("uri");
                if (uri != null && uri.startsWith("/events/")) {
                    Long id = Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
                    Object hits = stat.get("hits");
                    if (hits instanceof Number) {
                        views.put(id, ((Number) hits).intValue());
                    }
                }
            }

            eventIds.forEach(id -> views.putIfAbsent(id, 0));
            return views;

        } catch (Exception e) {
            return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0));
        }
    }
    private Pageable getPageable(String sort, int from, int size) {
        int page = from / size;
        if ("EVENT_DATE".equals(sort)) {
            return PageRequest.of(page, size, Sort.by("eventDateTime").descending());
        }
        return PageRequest.of(page, size);
    }

    private EventDto enrichEventDto(Event event) {
        UserShortDto initiator = getInitiator(event.getOwnerId());
        Long confirmed = getConfirmedMap(List.of(event.getId())).getOrDefault(event.getId(), 0L);
        Integer views = getViewsMap(List.of(event.getId())).getOrDefault(event.getId(), 0);

        return eventMapper.toEventDto(
                event,
                EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                initiator,
                confirmed,
                views
        );
    }

    private List<EventShortDto> enrichShortDtos(List<Event> events) {
        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<Long> ownerIds = events.stream().map(Event::getOwnerId).distinct().collect(Collectors.toList());

        Map<Long, Long> confirmed = getConfirmedMap(eventIds);
        Map<Long, UserShortDto> initiators = getInitiatorsMap(ownerIds);
        Map<Long, Integer> views = getViewsMap(eventIds);

        return events.stream()
                .map(e -> eventMapper.toEventShortDto(
                        e,
                        EventCategoryMapper.toCategoryDtoFromCategory(e.getCategory()),
                        initiators.getOrDefault(e.getOwnerId(), new UserShortDto(e.getOwnerId(), "Unknown")),
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0)
                ))
                .collect(Collectors.toList());
    }

    private List<EventDto> enrichFullDtos(List<Event> events) {
        if (events.isEmpty()) return List.of();

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<Long> ownerIds = events.stream().map(Event::getOwnerId).distinct().collect(Collectors.toList());

        Map<Long, Long> confirmed = getConfirmedMap(eventIds);
        Map<Long, UserShortDto> initiators = getInitiatorsMap(ownerIds);
        Map<Long, Integer> views = getViewsMap(eventIds);

        return events.stream()
                .map(e -> eventMapper.toEventDto(
                        e,
                        EventCategoryMapper.toCategoryDtoFromCategory(e.getCategory()),
                        initiators.getOrDefault(e.getOwnerId(), new UserShortDto(e.getOwnerId(), "Unknown")),
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0)
                ))
                .collect(Collectors.toList());
    }
}