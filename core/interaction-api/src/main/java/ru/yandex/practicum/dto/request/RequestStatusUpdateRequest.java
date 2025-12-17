package ru.yandex.practicum.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.yandex.practicum.enums.RequestStatus;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestStatusUpdateRequest {
    List<Long> requestIds;
    RequestStatus status;
}
