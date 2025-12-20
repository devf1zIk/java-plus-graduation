package ru.yandex.practicum.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.yandex.practicum.dto.event.CreateNewEventDto;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.event.UpdateEventAdminDto;
import ru.yandex.practicum.dto.event.UpdateEventUserRequest;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.EventCategory;
import ru.yandex.practicum.model.Location;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(
        componentModel = "spring",
        uses = {EventCategoryMapper.class, LocationMapper.class}
)
public interface EventMapper {

    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "rating", ignore = true)
    EventDto toFullDto(Event event);

    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "rating", ignore = true)
    EventShortDto toShortDto(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", source = "initiatorId")
    @Mapping(target = "initiatorId", source = "initiatorId")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "createdOn", expression = "java(mapNow())")
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "eventDateTime", source = "dto.eventDate", qualifiedByName = "localDateTimeToInstant")
    @Mapping(target = "paid", expression = "java(dto.getPaid() != null ? dto.getPaid() : false)")
    @Mapping(target = "participantLimit", expression = "java(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)")
    @Mapping(target = "isModerated", expression = "java(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)")
    Event toEvent(CreateNewEventDto dto, Long initiatorId, EventCategory category, Location location);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "eventDateTime", source = "eventDate", qualifiedByName = "localDateTimeToInstant")
    void patchFromUser(UpdateEventUserRequest src, @MappingTarget Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "eventDateTime", source = "eventDate", qualifiedByName = "localDateTimeToInstant")
    void patchFromAdmin(UpdateEventAdminDto src, @MappingTarget Event event);

    @Named("localDateTimeToInstant")
    default Instant map(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }

    default LocalDateTime map(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    default Instant mapNow() {
        return Instant.now();
    }
}