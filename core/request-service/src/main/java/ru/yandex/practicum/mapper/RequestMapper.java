package ru.yandex.practicum.mapper;

import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.model.ParticipationRequest;

@NoArgsConstructor
public class RequestMapper {

    public static ParticipationRequestDto fromRequestTpRequestDto(ParticipationRequest participationrequest) {
        return new ParticipationRequestDto(
                participationrequest.getCreatedOn(),
                participationrequest.getEventId(),
                participationrequest.getId(),
                participationrequest.getRequesterId(),
                participationrequest.getStatus());
    }
}