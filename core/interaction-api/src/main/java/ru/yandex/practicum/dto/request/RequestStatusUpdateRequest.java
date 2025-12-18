package ru.yandex.practicum.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.yandex.practicum.enums.RequestStatus;
import java.util.List;

@Data
public class RequestStatusUpdateRequest {
    @NotEmpty(message = "requestIds не может быть пустым")
    private List<Long> requestIds;

    @NotNull(message = "status не может быть null")
    private RequestStatus status;
}