package ru.yandex.practicum.event.mapper;


import ru.yandex.practicum.event.model.Compilation;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.interaction.dto.CompilationDto;
import ru.yandex.practicum.interaction.dto.EventShortDto;
import ru.yandex.practicum.interaction.dto.NewCompilationDto;

import java.util.List;
import java.util.Set;

public class CompilationMapper {

    public static Compilation toCompilationFromNewCompilationDto(NewCompilationDto newCompilationDto, Set<Event> events) {
        return Compilation.builder()
                .title(newCompilationDto.getTitle())
                .events(events)
                .pinned(newCompilationDto.isPinned())
                .build();
    }

    public static CompilationDto toCompilationDtoFromCompilation(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .events(events)
                .build();
    }
}