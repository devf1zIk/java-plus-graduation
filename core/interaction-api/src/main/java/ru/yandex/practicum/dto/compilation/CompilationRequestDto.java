package ru.yandex.practicum.dto.compilation;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import java.util.Set;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationRequestDto {
    Set<Long> events;
    Boolean pinned;
    @Size(min = 1, max = 50, message = "Название подборки должно быть от 1 до 50 символов")
    String title;
}
