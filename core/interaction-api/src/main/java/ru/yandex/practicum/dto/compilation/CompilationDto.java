package ru.yandex.practicum.dto.compilation;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.dto.event.EventShortDto;
import java.util.Set;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationDto {
    Set<EventShortDto> events;
    Long id;
    Boolean pinned;
    @Size(max = 50, message = "Название подборки должно быть до 50 символов")
    String title;
}
