package ru.yandex.practicum.mapper;

import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.event.CreateNewEventDto;
import ru.yandex.practicum.dto.event.EventCategoryDto;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.model.Event;
import ru.yandex.practicum.model.EventCategory;
import ru.yandex.practicum.model.User;
import java.time.ZoneId;
import java.time.ZoneOffset;
import static java.time.LocalDateTime.now;
import static java.time.LocalDateTime.ofInstant;

@NoArgsConstructor
public class EventMapper {


    public static Event fromCreateNewEventDtoToEvent(CreateNewEventDto newEventDto, User owner,
                                                     EventCategory category) {
        return new Event(null,
                newEventDto.getTitle(),
                newEventDto.getAnnotation(),
                newEventDto.getDescription(),
                category,
                now().toInstant(ZoneOffset.UTC),
                newEventDto.getEventDate().toInstant(ZoneOffset.UTC),
                owner,
                newEventDto.getLocation(),
                newEventDto.getPaid(),
                newEventDto.getParticipantLimit(),
                null,
                newEventDto.getRequestModeration(),
                EventState.PENDING);
    }

    public static EventDto fromEventToEventDto(Event event, EventCategoryDto eventCategoryDto, UserShortDto owner,
                                               Long confirmedRequests, Integer views) {
        EventDto eventDto = new EventDto(event.getId(),
                event.getAnnotation(),
                eventCategoryDto,
                confirmedRequests,
                ofInstant(event.getCreatedOn(), ZoneId.of("UTC")),
                event.getDescription(),
                ofInstant(event.getEventDateTime(), ZoneId.of("UTC")),
                owner,
                event.getLocation(),
                event.getIsPaid(),
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

    public static EventShortDto fromEventToEventShortDto(Event event, EventCategoryDto eventCategoryDto, UserShortDto owner,
                                                         Long confirmedRequests, Integer views) {
        return new EventShortDto(event.getId(),
                event.getAnnotation(),
                eventCategoryDto,
                confirmedRequests,
                ofInstant(event.getEventDateTime(), ZoneId.of("UTC")),
                owner,
                event.getIsPaid(),
                event.getTitle(),
                views);
    }
}
