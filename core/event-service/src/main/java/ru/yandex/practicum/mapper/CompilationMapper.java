package ru.yandex.practicum.mapper;

import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.CompilationRequestDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.model.Compilation;
import ru.yandex.practicum.model.Event;
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
