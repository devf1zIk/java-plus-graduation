package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.CompilationRequestDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.exception.model.BadRequestException;
import ru.yandex.practicum.exception.model.NotFoundException;
import ru.yandex.practicum.feign.request.RequestClient;
import ru.yandex.practicum.feign.user.UserClient;
import ru.yandex.practicum.mapper.CompilationMapper;
import ru.yandex.practicum.mapper.EventMapper;
import ru.yandex.practicum.model.Compilation;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.repository.CompilationRepository;
import ru.yandex.practicum.repository.EventRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventMapper eventMapper;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;

    public List<CompilationDto> getAll(boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations = compilationRepository.getAllByPinned(pinned, pageable).getContent();

        List<CompilationDto> result = new ArrayList<>();
        for (Compilation compilation : compilations) {
            Set<EventShortDto> items = getEventsShorts(compilation.getEvents());
            result.add(CompilationMapper.toDtoFromCompilation(compilation, items));
        }
        return result;
    }

    public CompilationDto getById(long id) {
        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + id + " не найдена"));

        Set<EventShortDto> items = getEventsShorts(compilation.getEvents());
        return CompilationMapper.toDtoFromCompilation(compilation, items);
    }

    public CompilationDto create(CompilationRequestDto dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BadRequestException("Title не может быть пустым");
        }

        Set<Event> events = new HashSet<>();
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            if (events.size() != dto.getEvents().size()) {
                throw new NotFoundException("Некоторые события не найдены");
            }
        }

        Compilation compilation = CompilationMapper.toCompilationFromDto(dto, events);
        if (dto.getPinned() == null) {
            compilation.setPinned(false);
        }

        Compilation saved = compilationRepository.save(compilation);
        Set<EventShortDto> items = getEventsShorts(saved.getEvents());
        return CompilationMapper.toDtoFromCompilation(saved, items);
    }

    public void delete(long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compilationId + " не существует"));
        compilationRepository.delete(compilation);
    }

    public CompilationDto updateCompilation(long compilationId, CompilationRequestDto dto) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compilationId + " не существует"));

        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            if (events.size() != dto.getEvents().size()) {
                throw new NotFoundException("Некоторые события не найдены");
            }
            compilation.setEvents(events);
        }

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            compilation.setTitle(dto.getTitle());
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        Compilation saved = compilationRepository.save(compilation);
        Set<EventShortDto> items = getEventsShorts(saved.getEvents());
        return CompilationMapper.toDtoFromCompilation(saved, items);
    }

    private Set<EventShortDto> getEventsShorts(Set<Event> events) {
        if (events == null || events.isEmpty()) {
            return Set.of();
        }

        List<Event> eventList = new ArrayList<>(events);
        List<Long> eventIds = eventList.stream().map(Event::getId).toList();
        List<Long> ownerIds = eventList.stream().map(Event::getOwnerId).distinct().toList();

        Map<Long, Long> confirmedMap = requestClient.getConfirmedCounts(eventIds);
        Map<Long, UserShortDto> initiatorMap = userClient.getByIds(ownerIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, u -> u));

        Set<EventShortDto> result = new HashSet<>();
        for (Event event : eventList) {
            EventShortDto dto = eventMapper.toShortDto(event);
            dto.setConfirmedRequests(confirmedMap.getOrDefault(event.getId(), 0L));
            dto.setInitiator(initiatorMap.get(event.getOwnerId()));
            result.add(dto);
        }
        return result;
    }
}