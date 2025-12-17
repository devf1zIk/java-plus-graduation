package ru.yandex.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.model.ParticipationRequest;

@UtilityClass
public class RequestMapper {

    public static ParticipationRequestDto fromRequestToRequestDto(ParticipationRequest participationrequest) {
        return new ParticipationRequestDto(
                participationrequest.getCreatedOn(),
                participationrequest.getEventId(),
                participationrequest.getId(),
                participationrequest.getRequesterId(),
                participationrequest.getStatus());
    }
}