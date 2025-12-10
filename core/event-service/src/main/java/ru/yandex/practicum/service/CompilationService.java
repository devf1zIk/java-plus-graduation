package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.StatsClient;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.CompilationRequestDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.exception.model.BadRequestException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.mapper.CompilationMapper;
import ru.yandex.practicum.mapper.EventCategoryMapper;
import ru.yandex.practicum.mapper.EventMapper;
import ru.yandex.practicum.mapper.UserMapper;
import ru.yandex.practicum.model.Compilation;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.repository.CompilationRepository;
import ru.yandex.practicum.repository.EventRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;

    private final StatsClient statClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public CompilationService(CompilationRepository compilationRepository, EventRepository eventRepository,
                              EventService eventService, StatsClient statClient) {
        this.compilationRepository = compilationRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.statClient = statClient;
    }

    public List<CompilationDto> getAll(boolean pinned, Pageable pageable) {
        List<Compilation> compilations = compilationRepository
                .getAllByPinned(pinned, pageable).stream().toList();
        List<CompilationDto> result = new ArrayList<>();
        for (Compilation compilation : compilations) {
            Set<EventShortDto> items = new HashSet<>();
            if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
                Set<Event> eventSet = compilation.getEvents();
                items = getEventsShorts(eventSet);
            }
            result.add(CompilationMapper.toDtoFromCompilation(compilation, items));
        }

        return result;
    }

    public CompilationDto getById(long id) {
        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + id + " не найдена в БД"));

        Set<EventShortDto> items = new HashSet<>();

        if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
            Set<Event> eventSet = compilation.getEvents();
            items = getEventsShorts(eventSet);
        }

        return CompilationMapper.toDtoFromCompilation(compilation, items);
    }

    public CompilationDto create(CompilationRequestDto compilationDto) {
        Set<Long> eventIds = new HashSet<>();
        if (compilationDto.getEvents() != null) {
            eventIds.addAll(compilationDto.getEvents());
        }
        if (compilationDto.getTitle() == null || compilationDto.getTitle().isEmpty() || compilationDto.getTitle().isBlank()) {
            throw new BadRequestException("Title не может быть пустым");
        }
        Set<Event> eventSet = new HashSet<>();
        Set<EventShortDto> items = new HashSet<>();
        if (!eventIds.isEmpty()) {
            eventSet = new HashSet<>(eventRepository.findAllById(eventIds));
            if (eventIds.size() == eventSet.size()) {
                items = getEventsShorts(eventSet);
            } else {
                throw new NotFoundException("Некоторые события не найдены");
            }
        }

        Compilation compilationToSave = CompilationMapper.toCompilationFromDto(compilationDto, eventSet);
        if (compilationDto.getPinned() == null) {
            compilationToSave.setPinned(false);
        }
        Compilation compilation = compilationRepository.save(compilationToSave);

        return CompilationMapper.toDtoFromCompilation(compilation, items);
    }

    public void delete(long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compilationId + " не существует!"));
        compilationRepository.delete(compilation);
    }

    public CompilationDto updateCompilation(long compilationId, CompilationRequestDto updateCompilationRequest) {
        Compilation existedCompilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compilationId + " не существует!"));
        Set<Event> eventSet;
        if (updateCompilationRequest.getEvents() != null && !updateCompilationRequest.getEvents().isEmpty()) {
            eventSet = new HashSet<>(eventRepository.findAllById(updateCompilationRequest.getEvents()));
            if (updateCompilationRequest.getEvents().size() == eventSet.size()) {
                existedCompilation.setEvents(eventSet);
            } else {
                throw new NotFoundException("Некоторые события не найдены");
            }
        }
        eventSet = existedCompilation.getEvents();
        if (updateCompilationRequest.getTitle() != null && !updateCompilationRequest.getTitle().isBlank()) {
            existedCompilation.setTitle(updateCompilationRequest.getTitle());
        }
        if (updateCompilationRequest.getPinned() != null) {
            existedCompilation.setPinned(updateCompilationRequest.getPinned());
        }
        Set<EventShortDto> eventShortDtos = getEventsShorts(eventSet);
        Compilation resultCompilation = compilationRepository.save(existedCompilation);

        return CompilationMapper.toDtoFromCompilation(resultCompilation, eventShortDtos);
    }

    public Map<Long, Integer> getEventsViewsMap(List<Long> eventsIds) {
        if (eventsIds == null || eventsIds.isEmpty()) {
            return new HashMap<>();
        }

        List<String> uris = eventsIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        ResponseEntity<Object> response = statClient.getStats(
                "2000-01-01 00:00:00",
                LocalDateTime.now().format(formatter),
                uris,
                false
        );

        Map<Long, Integer> viewsMap = new HashMap<>();

        if (response.getBody() instanceof List<?> rawList && !rawList.isEmpty()) {
            for (Object item : rawList) {
                if (!(item instanceof Map<?, ?> map)) continue;

                String uri = (String) map.get("uri");
                if (uri == null || !uri.startsWith("/events/")) continue;

                String[] parts = uri.split("/");
                if (parts.length <= 2) continue;

                String idStr = parts[parts.length - 1];
                try {
                    Long eventId = Long.parseLong(idStr);
                    if (eventsIds.contains(eventId)) {
                        Integer hits = map.get("hits") instanceof Number n ? n.intValue() : 0;
                        viewsMap.put(eventId, hits);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        eventsIds.forEach(id -> viewsMap.putIfAbsent(id, 0));
        return viewsMap;
    }

    private Set<EventShortDto> getEventsShorts(Set<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequestsCountForEvents = eventService
                .getConfirmedRequestsCountForEvents(new ArrayList<>(events));
        Map<Long, Integer> viewsMap = getEventsViewsMap(new ArrayList<>(eventIds));

        return events.stream()
                .map(event -> EventMapper.fromEventToEventShortDto(event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        UserMapper.fromUserToUserShortDto(event.getOwner()),
                        confirmedRequestsCountForEvents.getOrDefault(event.getId(), 0L),
                        viewsMap.get(event.getId()))).collect(Collectors.toSet());
    }
}
