package ru.yandex.practicum.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.event.*;
import java.util.List;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient extends EventOperations {

    @Override
    @GetMapping("/events/{id}")
    EventDto getPublicEvent(@PathVariable("id") Long id);

    @Override
    @GetMapping("/events")
    List<EventShortDto> getPublicEvents(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "categories", required = false) List<Long> categories,
            @RequestParam(value = "paid", required = false) Boolean paid,
            @RequestParam(value = "rangeStart", required = false) String rangeStart,
            @RequestParam(value = "rangeEnd", required = false) String rangeEnd,
            @RequestParam(value = "onlyAvailable", defaultValue = "false") boolean onlyAvailable,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "from", defaultValue = "0") int from,
            @RequestParam(value = "size", defaultValue = "10") int size
    );

    @Override
    @GetMapping("/users/{userId}/events")
    List<EventShortDto> getUserEvents(@PathVariable("userId") Long userId,
                                      @RequestParam("from") int from,
                                      @RequestParam("size") int size);

    @Override
    @PostMapping("/users/{userId}/events")
    EventDto createEvent(@PathVariable("userId") Long userId,
                         @RequestBody CreateNewEventDto dto);

    @Override
    @GetMapping("/users/{userId}/events/{eventId}")
    EventDto getUserEvent(@PathVariable("userId") Long userId,
                          @PathVariable("eventId") Long eventId);

    @Override
    @PatchMapping("/users/{userId}/events/{eventId}")
    EventDto updateEventByUser(@PathVariable("userId") Long userId,
                               @PathVariable("eventId") Long eventId,
                               @RequestBody UpdateEventUserRequest dto);

    @Override
    @GetMapping("/admin/events")
    List<EventDto> getAdminEvents(@RequestParam(value = "users", required = false) List<Long> users,
                                  @RequestParam(value = "states", required = false) List<String> states,
                                  @RequestParam(value = "categories", required = false) List<Long> categories,
                                  @RequestParam(value = "rangeStart", required = false) String rangeStart,
                                  @RequestParam(value = "rangeEnd", required = false) String rangeEnd,
                                  @RequestParam(value = "from", defaultValue = "0") int from,
                                  @RequestParam(value = "size", defaultValue = "10") int size);

    @Override
    @PatchMapping("/admin/events/{eventId}")
    EventDto updateEventByAdmin(@PathVariable("eventId") Long eventId,
                                @RequestBody UpdateEventAdminDto dto);
}
