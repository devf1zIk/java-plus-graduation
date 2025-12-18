package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ru.yandex.practicum.dto.category.EventCategoryDto;
import ru.yandex.practicum.dto.event.CreateNewEventDto;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.EventCategory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(
        componentModel = "spring",
        uses = LocationMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface EventMapper {

    /* ===================== CREATE ===================== */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(
            target = "createdOn",
            expression = "java(java.time.Instant.now())"
    )
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", constant = "PENDING")

    @Mapping(
            target = "paid",
            expression = "java(dto.getPaid() != null ? dto.getPaid() : false)"
    )
    @Mapping(
            target = "participantLimit",
            expression = "java(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0L)"
    )
    @Mapping(
            target = "isModerated",
            expression = "java(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)"
    )

    @Mapping(target = "eventDateTime", source = "dto.eventDate")
    @Mapping(target = "location", source = "dto.location")

    Event toEvent(CreateNewEventDto dto, Long ownerId, EventCategory category);

    /* ===================== FULL DTO ===================== */

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "category", source = "categoryDto")
    @Mapping(target = "eventDate", source = "event.eventDateTime")
    @Mapping(target = "initiator", source = "owner")
    @Mapping(target = "requestModeration", source = "event.isModerated")
    EventDto toEventDto(
            Event event,
            EventCategoryDto categoryDto,
            UserShortDto owner,
            Long confirmedRequests,
            Integer views
    );

    /* ===================== SHORT DTO ===================== */

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "category", source = "categoryDto")
    @Mapping(target = "eventDate", source = "event.eventDateTime")
    @Mapping(target = "initiator", source = "owner")
    EventShortDto toEventShortDto(
            Event event,
            EventCategoryDto categoryDto,
            UserShortDto owner,
            Long confirmedRequests,
            Integer views
    );

    /* ===================== DATE ===================== */

    default Instant map(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }

    default LocalDateTime map(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
