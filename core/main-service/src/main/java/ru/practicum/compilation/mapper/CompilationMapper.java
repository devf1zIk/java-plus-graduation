package ru.practicum.compilation.mapper;

import lombok.NoArgsConstructor;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.CompilationRequestDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;
import java.util.Set;

@NoArgsConstructor
public class CompilationMapper {

    public static CompilationDto toDtoFromCompilation(Compilation compilation, Set<EventShortDto> eventDto) {

        return new CompilationDto(eventDto, compilation.getId(), compilation.getPinned(), compilation.getTitle());
    }

    public static Compilation toCompilationFromDto(CompilationRequestDto compilationDto, Set<Event> eventsList) {

        return new Compilation(null, eventsList, compilationDto.getPinned(), compilationDto.getTitle());
    }
}
