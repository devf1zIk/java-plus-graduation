package ru.yandex.practicum.interaction.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.interaction.config.FeignConfig;
import ru.yandex.practicum.interaction.dto.ParticipationRequestDto;
import ru.yandex.practicum.interaction.enums.ParticipationRequestStatus;
import ru.yandex.practicum.interaction.fallback.ParticipationRequestFallback;

import java.util.List;

@FeignClient(name = "request-service",
        path = "/internal/events/requests",
        configuration = FeignConfig.class,
        fallback = ParticipationRequestFallback.class)
public interface ParticipationRequestClient {

    @GetMapping("/byEventId")
    List<ParticipationRequestDto> getAllByEventId(@RequestParam Long eventId);

    @GetMapping("/byEventIdsAndStatus")
    List<ParticipationRequestDto> getAllByEventIdsAndStatus(@RequestParam List<Long> eventIds,
                                                            @RequestParam ParticipationRequestStatus status);

    @GetMapping("/count")
    long countByEventIdAndStatus(@RequestParam Long eventId,
                                 @RequestParam ParticipationRequestStatus status);


    @PutMapping
    Integer updateAllRequests(@RequestBody List<ParticipationRequestDto> updatedRequests);

}
