package ru.yandex.practicum.dto.compilation;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.Set;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CompilationRequestDto {

    Set<Long> events;

    Boolean pinned;

    @Size(max = 50)
    String title;
}
