package ru.practicum.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.practicum.event.dto.EventShortDto;
import java.util.Set;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CompilationDto {
    private Set<EventShortDto> events;
    private Long id;
    private Boolean pinned;
    @Size(max = 50)
    private String title;
}
