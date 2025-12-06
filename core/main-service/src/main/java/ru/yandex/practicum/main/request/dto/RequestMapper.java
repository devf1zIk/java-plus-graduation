package ru.yandex.practicum.main.request.dto;

import lombok.NoArgsConstructor;
import ru.yandex.practicum.main.request.model.ParticipationRequest;

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