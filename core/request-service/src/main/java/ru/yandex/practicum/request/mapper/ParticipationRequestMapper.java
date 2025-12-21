package ru.yandex.practicum.request.mapper;

import ru.yandex.practicum.interaction.dto.ParticipationRequestDto;
import ru.yandex.practicum.request.model.ParticipationRequest;

import java.util.List;

public class ParticipationRequestMapper {

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus())
                .build();
    }

    public static ParticipationRequest toEntity(ParticipationRequestDto dto) {
        return ParticipationRequest.builder()
                .id(dto.getId())
                .created(dto.getCreated())
                .status(dto.getStatus())
                .requesterId(dto.getRequester())
                .eventId(dto.getEvent())
                .build();
    }

    public static List<ParticipationRequest> toEntityList(List<ParticipationRequestDto> dtoList) {
        return dtoList.stream()
                .map(ParticipationRequestMapper::toEntity)
                .toList();
    }

    public static List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> list) {
        return list.stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }
}