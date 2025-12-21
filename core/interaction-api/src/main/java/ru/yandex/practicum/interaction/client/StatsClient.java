package ru.yandex.practicum.interaction.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.interaction.config.FeignConfig;
import ru.yandex.practicum.interaction.dto.EndpointHitDto;
import ru.yandex.practicum.interaction.dto.ViewStatsDto;
import ru.yandex.practicum.interaction.fallback.StatsFallback;

import java.util.List;

@FeignClient(name = "stats-server",
        path = "/",
        configuration = FeignConfig.class,
        fallback = StatsFallback.class)
public interface StatsClient {

    @PostMapping("/hit")
    void sendHit(@RequestBody EndpointHitDto hitDto);

    @GetMapping("/stats")
    List<ViewStatsDto> getStats(@RequestParam String start,
                                @RequestParam String end,
                                @RequestParam(required = false) List<String> uris,
                                @RequestParam(defaultValue = "false") boolean unique);
}