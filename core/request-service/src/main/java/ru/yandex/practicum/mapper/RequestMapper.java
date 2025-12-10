package ru.yandex.practicum.mapper;

import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.model.ParticipationRequest;

@NoArgsConstructor
public class RequestMapper {

    public static RequestDto fromRequestTpRequestDto(ParticipationRequest participationrequest) {
        return new RequestDto(
                participationrequest.getCreatedOn(),
                participationrequest.getEvent().getId(),
                participationrequest.getId(),
                participationrequest.getRequester().getId(),
                participationrequest.getStatus());
    }
}