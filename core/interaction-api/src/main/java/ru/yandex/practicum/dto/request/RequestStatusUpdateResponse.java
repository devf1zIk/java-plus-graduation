package ru.yandex.practicum.dto.request;

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
public class RequestStatusUpdateResponse {
    Set<ParticipationRequestDto> confirmedRequests;
    Set<ParticipationRequestDto> rejectedRequests;
}
