package ru.yandex.practicum.main.event.mapper;

import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.main.category.model.EventCategory;
import ru.yandex.practicum.dto.event.NewEventDto;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.main.event.model.Event;
import java.time.ZoneId;
import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.ofInstant;

@NoArgsConstructor
public class EventMapper {

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    public static Event fromCreateNewEventDtoToEvent(NewEventDto newEventDto,
                                                     EventCategory category,Long userId) {
        return Event.builder()
                .title(newEventDto.getTitle())
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .category(category)
                .createdOn(now().atZone(UTC_ZONE).toInstant())
                .eventDateTime(newEventDto.getEventDate().atZone(UTC_ZONE).toInstant())
                .initiatorId(userId)
                .location(LocationMapper.location(newEventDto.getLocation()))
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0L)
                .publishedOn(null)
                .isModerated(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true)
                .state(EventState.PENDING)
                .build();
    }

    public static EventDto fromEventToEventDto(Event event, CategoryDto eventCategoryDto, UserShortDto owner,
                                               Long confirmedRequests, Integer views) {
        EventDto eventDto = new EventDto(event.getId(),
                event.getAnnotation(),
                eventCategoryDto,
                confirmedRequests,
                ofInstant(event.getCreatedOn(), ZoneId.of("UTC")),
                event.getDescription(),
                ofInstant(event.getEventDateTime(), ZoneId.of("UTC")),
                owner,
                LocationMapper.locationDto(event.getLocation()),
                event.getPaid(),
                event.getParticipantLimit(),
                null,
                event.getIsModerated(),
                event.getState(),
                event.getTitle(),
                views
        );
        if (event.getPublishedOn() != null) {
            eventDto.setPublishedOn(ofInstant(event.getPublishedOn(), ZoneId.of("UTC")));
        }
        return eventDto;
    }

    public static EventShortDto fromEventToEventShortDto(Event event, CategoryDto eventCategoryDto, UserShortDto owner,
                                                         Long confirmedRequests, Integer views) {
        return new EventShortDto(event.getId(),
                event.getAnnotation(),
                eventCategoryDto,
                confirmedRequests,
                ofInstant(event.getEventDateTime(), ZoneId.of("UTC")),
                owner,
                event.getPaid(),
                event.getTitle(),
                views);
    }
}
