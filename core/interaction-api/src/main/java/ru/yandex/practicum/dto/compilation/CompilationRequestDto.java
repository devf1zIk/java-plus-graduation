package ru.yandex.practicum.dto.compilation;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationRequestDto {
    Set<Long> events;
    Boolean pinned;
    @Size(max = 50, message = "Название подборки должно быть до 50 символов")
    String title;
}
