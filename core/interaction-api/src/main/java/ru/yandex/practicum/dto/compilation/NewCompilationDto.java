package ru.yandex.practicum.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewCompilationDto {

    @Builder.Default
    Boolean pinned = false;

    @NotBlank(message = "Название подборки не может быть пустым")
    @Size(max = 50, message = "Название подборки должно быть до 50 символов")
    String title;

    List<Long> events;

}
