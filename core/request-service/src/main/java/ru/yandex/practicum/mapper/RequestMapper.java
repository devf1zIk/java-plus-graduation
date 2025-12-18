package ru.yandex.practicum.mapper;

import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.model.ParticipationRequest;

@NoArgsConstructor
public class RequestMapper {

    public static RequestDto fromRequestTpRequestDto(ParticipationRequest participationrequest) {
        if (participationrequest == null) {
            return null;
        }
        return new RequestDto(
                participationrequest.getCreatedOn() != null ? participationrequest.getCreatedOn() : java.time.LocalDateTime.now(),
                participationrequest.getEventId() != null ? participationrequest.getEventId() : 0L,
                participationrequest.getId() != null ? participationrequest.getId() : 0L,
                participationrequest.getRequesterId() != null ? participationrequest.getRequesterId() : 0L,
                participationrequest.getStatus() != null ? participationrequest.getStatus() : ru.yandex.practicum.enums.RequestStatus.PENDING
        );
    }
}