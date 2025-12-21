package ru.yandex.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.interaction.client.StatsClient;
import ru.yandex.practicum.interaction.dto.EndpointHitDto;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/main")
@RequiredArgsConstructor
public class MainController {
    private final StatsClient statsClient;

    @PostMapping("/track")
    public void trackEvent(@RequestParam String uri, @RequestParam String ip) {
        EndpointHitDto hitDto = new EndpointHitDto("event-service", uri, ip, LocalDateTime.now());
        statsClient.sendHit(hitDto);
    }
}
