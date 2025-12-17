package ru.yandex.practicum.main.compilation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.StatsClient;
import ru.yandex.practicum.client.request.RequestClient;
import ru.yandex.practicum.client.user.UserClient;
import ru.yandex.practicum.dto.compilation.NewCompilationDto;
import ru.yandex.practicum.enums.RequestStatus;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.main.category.mapper.EventCategoryMapper;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.CompilationRequestDto;
import ru.yandex.practicum.main.compilation.mapper.CompilationMapper;
import ru.yandex.practicum.main.compilation.model.Compilation;
import ru.yandex.practicum.main.compilation.repository.CompilationRepository;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.main.event.mapper.EventMapper;
import ru.yandex.practicum.main.event.model.Event;
import ru.yandex.practicum.main.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final StatsClient statClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public CompilationService(CompilationRepository compilationRepository, EventRepository eventRepository,
                              RequestClient requestClient, UserClient userClient, StatsClient statClient) {
        this.compilationRepository = compilationRepository;
        this.eventRepository = eventRepository;
        this.requestClient = requestClient;
        this.userClient = userClient;
        this.statClient = statClient;
    }

    public List<CompilationDto> getAll(boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
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

    public CompilationDto getCompilationById(Long id) {
        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + id + " не найдена в БД"));

        Set<EventShortDto> items = new HashSet<>();

        if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
            Set<Event> eventSet = compilation.getEvents();
            items = getEventsShorts(eventSet);
        }

        return CompilationMapper.toDtoFromCompilation(compilation, items);
    }

    public CompilationDto create(NewCompilationDto newCompilationDto) {
        Set<Long> eventIds = new HashSet<>();
        if (newCompilationDto.getEvents() != null) {
            eventIds.addAll(newCompilationDto.getEvents());
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

        Compilation compilationToSave = CompilationMapper.toCompilationFromDto(newCompilationDto, eventSet);
        if (newCompilationDto.getPinned() == null) {
            compilationToSave.setPinned(false);
        }
        Compilation compilation = compilationRepository.save(compilationToSave);

        return CompilationMapper.toDtoFromCompilation(compilation, items);
    }

    public void deleteCompilation(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compilationId + " не существует!"));
        compilationRepository.delete(compilation);
    }

    public CompilationDto updateCompilation(Long compilationId, CompilationRequestDto updateCompilationRequest) {
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
        Map<Long, Integer> viewsMap = getEventsViewsMap(new ArrayList<>(eventIds));

        return events.stream()
                .map(event -> EventMapper.fromEventToEventShortDto(event,
                        EventCategoryMapper.toCategoryDtoFromCategory(event.getCategory()),
                        userClient.getUser(event.getInitiatorId()),
                        requestClient.countByStatus(event.getId(), RequestStatus.CONFIRMED),
                        viewsMap.get(event.getId()))).collect(Collectors.toSet());
    }
}
