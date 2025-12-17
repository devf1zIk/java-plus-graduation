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
    private Set<Long> events;
    private Boolean pinned;
    @Size(min = 1, max = 50, message = "Название подборки должно быть от 1 до 50 символов")
    private String title;
}
