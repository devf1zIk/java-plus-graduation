package ru.yandex.practicum.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewCompilationDto {

    @Builder.Default
    Boolean pinned = false;

    @NotBlank(message = "Title не может быть пустым")
    @Size(max = 50, message = "Название подборки должно быть до 50 символов")
    String title;

    List<Long> events;

}
