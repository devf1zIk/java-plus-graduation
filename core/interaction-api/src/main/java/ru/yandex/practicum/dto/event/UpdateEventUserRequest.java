package ru.yandex.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ru.yandex.practicum.enums.UserEventActions;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class UpdateEventUserRequest {

    @Size(min = 20, max = 7000, message = "Описание должно быть длиной от 20 до 7000 символов")
    private String annotation;
    private Long category;
    @Size(min = 20, max = 7000, message = "Описание должно быть длиной от 20 до 7000 символов")
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Future(message = "Дата события должна быть в будущем")
    private LocalDateTime eventDate;
    private LocationDto location;
    private Boolean paid;
    @PositiveOrZero(message = "Лимит участников должен быть положительным числом или нулём")
    private Long participantLimit;
    private Boolean requestModeration;
    private UserEventActions stateAction;
    @Size(min = 3, max = 120, message = "Название должно быть длиной от 3 до 120 символов")
    private String title;
}
