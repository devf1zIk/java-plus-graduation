package ru.yandex.practicum.dto.compilation;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.dto.event.EventShortDto;
import java.util.Set;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CompilationDto {
    Set<EventShortDto> events;

    Long id;

    Boolean pinned;

    @Size(max = 50)
    String title;
}
