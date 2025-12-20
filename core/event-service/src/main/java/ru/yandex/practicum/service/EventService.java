package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.ActionType;
import ru.yandex.practicum.client.AnalyzerClient;
import ru.yandex.practicum.client.CollectorClient;
import ru.yandex.practicum.dto.event.*;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.*;
import ru.yandex.practicum.exception.model.*;
import ru.yandex.practicum.feign.request.RequestClient;
import ru.yandex.practicum.feign.request.RequestClientSingle;
import ru.yandex.practicum.feign.user.UserClient;
import ru.yandex.practicum.mapper.EventMapper;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.EventCategory;
import ru.yandex.practicum.model.Location;
import ru.yandex.practicum.repository.*;
import ru.yandex.practicum.stats.proto.RecommendedEventProto;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final RequestClientSingle requestClientSingle;
    private final EventMapper eventMapper;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;

    @Transactional
    public EventDto create(CreateNewEventDto dto, Long userId) {
        userClient.getById(userId);
        validateEventDate(dto.getEventDate());

        EventCategory category = getCategory(dto.getCategory());
        Location location = saveLocation(dto.getLocation());

        Event event = eventMapper.toEvent(dto, userId, category, location);
        return enrichWithDetails(eventRepository.save(event));
    }

    @Transactional
    public EventDto updateByAdmin(Long eventId, UpdateEventAdminDto dto) {
        Event event = getEvent(eventId);
        validateEventDate(dto.getEventDate());

        if (dto.getStateAction() == AdminEventAction.PUBLISH_EVENT && event.getState() != EventState.PENDING) {
            throw new ConflictException("Опубликовать можно только событие в ожидании модерации");
        }
        if (dto.getStateAction() == AdminEventAction.REJECT_EVENT && event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя отклонить опубликованное событие");
        }

        eventMapper.patchFromAdmin(dto, event);
        handleAdminStateAction(event, dto.getStateAction());
        return enrichWithDetails(eventRepository.save(event));
    }

    public List<EventDto> getAll(List<Long> users, List<String> states, List<Long> categories,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Specification<Event> spec = buildAdminSpec(users, states, categories, rangeStart, rangeEnd);

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        return enrichListWithDetails(events);
    }

    public List<EventShortDto> getAllShort(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd, boolean onlyAvailable,
                                           String sort, int from, int size) {
        Pageable pageable = getPageable(sort, from, size);
        Specification<Event> spec = buildPublicSpec(text, categories, paid, rangeStart, rangeEnd);

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        if (events.isEmpty()) {
            return new ArrayList<>();
        }

        if (onlyAvailable) {
            events = filterAvailableEvents(events);
        }

        return enrichListWithShortDetails(events);
    }

    public EventDto getPublicEvent(Long eventId, Long userId) {
        Event event = getEvent(eventId);
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }

        collectorClient.collectUserActions(userId, eventId, ActionType.ACTION_VIEW);
        return enrichWithDetails(event);
    }

    public void likeEvent(Long userId, Long eventId) {
        Event event = getEvent(eventId);
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new BadRequestException("Нельзя лайкать неопубликованное событие");
        }

        boolean hasVisited = requestClientSingle.hasVisitedEvent(userId, eventId);
        if (!hasVisited) {
            throw new BadRequestException("Пользователь может лайкать только посещённые им мероприятия");
        }

        collectorClient.collectUserActions(userId, eventId, ActionType.ACTION_LIKE);
    }

    public List<EventShortDto> getRecommendations(Long userId, int size) {
        List<RecommendedEventProto> recommendations = analyzerClient.getRecommendationsForUser(userId, size).toList();
        List<EventShortDto> result = new ArrayList<>();

        for (RecommendedEventProto rec : recommendations) {
            Event event = getEvent(rec.getEventId());
            EventShortDto dto = eventMapper.toShortDto(event);

            dto.setRating(rec.getScore());
            dto.setConfirmedRequests(requestClient.getConfirmedCounts(List.of(event.getId())).getOrDefault(event.getId(), 0L));
            dto.setInitiator(userClient.getById(event.getOwnerId()));

            result.add(dto);
        }
        return result;
    }

    public List<EventShortDto> getByUserId(Long userId, Pageable pageable) {
        userClient.getById(userId);
        return enrichListWithShortDetails(eventRepository.findAllByOwnerId(userId, pageable).getContent());
    }

    public EventDto getEventByUserId(Long userId, Long eventId) {
        userClient.getById(userId);
        Event event = getEvent(eventId);
        if (!event.getOwnerId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }
        return enrichWithDetails(event);
    }

    @Transactional
    public EventDto updateByUser(UpdateEventUserRequest dto, Long userId, Long eventId) {
        userClient.getById(userId);
        Event event = getEvent(eventId);

        if (!event.getOwnerId().equals(userId)) {
            throw new NotFoundException("Событие не принадлежит пользователю");
        }
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Изменить можно только ожидающее или отменённое событие");
        }

        eventMapper.patchFromUser(dto, event);
        handleUserStateAction(event, dto.getStateAction());
        return enrichWithDetails(eventRepository.save(event));
    }


    private EventDto enrichWithDetails(Event event) {
        EventDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(requestClient.getConfirmedCounts(List.of(event.getId())).getOrDefault(event.getId(), 0L));
        dto.setInitiator(userClient.getById(event.getOwnerId()));
        return dto;
    }

    private List<EventDto> enrichListWithDetails(List<Event> events) {
        if (events.isEmpty()) return new ArrayList<>();
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<Long> ownerIds = events.stream().map(Event::getOwnerId).distinct().toList();

        Map<Long, Long> confirmedMap = requestClient.getConfirmedCounts(eventIds);
        Map<Long, UserShortDto> initiatorMap = userClient.getByIds(ownerIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, u -> u));

        List<EventDto> result = new ArrayList<>();
        for (Event e : events) {
            EventDto dto = eventMapper.toFullDto(e);
            dto.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0L));
            dto.setInitiator(initiatorMap.get(e.getOwnerId()));
            result.add(dto);
        }
        return result;
    }

    private List<EventShortDto> enrichListWithShortDetails(List<Event> events) {
        if (events.isEmpty()) return new ArrayList<>();
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<Long> ownerIds = events.stream().map(Event::getOwnerId).distinct().toList();

        Map<Long, Long> confirmedMap = requestClient.getConfirmedCounts(eventIds);
        Map<Long, UserShortDto> initiatorMap = userClient.getByIds(ownerIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, u -> u));

        List<EventShortDto> result = new ArrayList<>();
        for (Event e : events) {
            EventShortDto dto = eventMapper.toShortDto(e);
            dto.setConfirmedRequests(confirmedMap.getOrDefault(e.getId(), 0L));
            dto.setInitiator(initiatorMap.get(e.getOwnerId()));
            result.add(dto);
        }
        return result;
    }

    private List<Event> filterAvailableEvents(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedMap = requestClient.getConfirmedCounts(eventIds);
        List<Event> availableEvents = new ArrayList<>();
        for (Event e : events) {
            if (e.getParticipantLimit() == 0 || confirmedMap.getOrDefault(e.getId(), 0L) < e.getParticipantLimit()) {
                availableEvents.add(e);
            }
        }
        return availableEvents;
    }

    private void handleAdminStateAction(Event event, AdminEventAction stateAction) {
        if (stateAction == AdminEventAction.PUBLISH_EVENT) {
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(java.time.Instant.now());
        } else if (stateAction == AdminEventAction.REJECT_EVENT) {
            event.setState(EventState.CANCELED);
        }
    }

    private void handleUserStateAction(Event event, UserEventActions stateAction) {
        if (stateAction == UserEventActions.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (stateAction == UserEventActions.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }
    }

    private Specification<Event> buildAdminSpec(List<Long> users, List<String> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Specification<Event> spec = Specification.where(null);
        if (users != null && !users.isEmpty()) spec = spec.and(EventSpecification.hasUsers(users));
        if (states != null && !states.isEmpty()) {
            List<EventState> eventStates = states.stream().map(EventState::valueOf).toList();
            spec = spec.and(EventSpecification.hasStates(eventStates));
        }
        if (categories != null && !categories.isEmpty()) spec = spec.and(EventSpecification.hasCategories(categories));
        if (rangeStart != null) spec = spec.and(EventSpecification.hasRangeStart(rangeStart.atZone(java.time.ZoneOffset.UTC).toInstant()));
        if (rangeEnd != null) spec = spec.and(EventSpecification.hasRangeEnd(rangeEnd.atZone(java.time.ZoneOffset.UTC).toInstant()));
        return spec;
    }

    private Specification<Event> buildPublicSpec(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        Specification<Event> spec = EventSpecification.isPublished();
        if (text != null && !text.isBlank()) spec = spec.and(EventSpecification.hasText(text));
        if (categories != null && !categories.isEmpty()) spec = spec.and(EventSpecification.hasCategories(categories));
        if (paid != null) spec = spec.and(EventSpecification.isPaid(paid));
        if (rangeStart != null) spec = spec.and(EventSpecification.hasRangeStart(rangeStart.atZone(java.time.ZoneOffset.UTC).toInstant()));
        if (rangeEnd != null) spec = spec.and(EventSpecification.hasRangeEnd(rangeEnd.atZone(java.time.ZoneOffset.UTC).toInstant()));
        return spec;
    }

    public Event getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + id + " не найдено"));
    }

    private EventCategory getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не найдена"));
    }

    private void validateEventDate(LocalDateTime time) {
        if (time != null && time.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }
    }

    private Location saveLocation(LocationDto dto) {
        if (dto == null) return null;
        return locationRepository.findByLatAndLon(dto.getLat(), dto.getLon())
                .orElseGet(() -> locationRepository.save(new Location(null, dto.getLat(), dto.getLon())));
    }

    private Pageable getPageable(String sort, int from, int size) {
        int page = from / size;
        return "EVENT_DATE".equals(sort)
                ? PageRequest.of(page, size, Sort.by("eventDateTime").descending())
                : PageRequest.of(page, size);
    }
}