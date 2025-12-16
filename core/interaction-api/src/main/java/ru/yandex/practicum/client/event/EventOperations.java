package ru.yandex.practicum.client.event;

import feign.FeignException;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.event.EventDto;

import java.util.List;

public interface EventOperations {

    @GetMapping("/admin/events/{eventId}")
    EventDto getEventById(@PathVariable @NotNull Long eventId);

    @GetMapping("/events/{eventId}")
    EventDto getPublicEventById(@PathVariable @NotNull Long eventId);

    @GetMapping("/admin/events/{eventId}/full")
    EventDto getEventFullById(@PathVariable long eventId) throws FeignException;

    @GetMapping("/users/{userId}/events/{eventId}/optional")
    EventDto getEventByIdAndInitiatorId(@PathVariable long userId, @PathVariable long eventId) throws FeignException;

    @GetMapping("/users/{userId}/events/initiator")
    List<EventDto> getAllEventByInitiatorId(@PathVariable long userId) throws FeignException;
}
