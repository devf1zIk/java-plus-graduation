package ru.yandex.practicum.interaction.dto;

import lombok.*;
import ru.yandex.practicum.interaction.enums.RequestStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {

    private List<Long> requestIds;

    private RequestStatus status;
}