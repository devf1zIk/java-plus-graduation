package ru.yandex.practicum.interaction.fallback;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.interaction.client.StatsClient;
import ru.yandex.practicum.interaction.dto.EndpointHitDto;
import ru.yandex.practicum.interaction.dto.ViewStatsDto;

import java.util.Collections;
import java.util.List;

@Slf4j
public class StatsFallback implements StatsClient {

    @Override
    public void sendHit(EndpointHitDto hitDto) {
        log.warn("[StatsClient#sendHit] Fallback triggered. Stats-server is unavailable. " +
                "Hit was not sent. Data: {}", hitDto);
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        log.warn("[StatsClient#getStats] Fallback triggered. Stats-server is unavailable. " +
                        "Returning empty stats. start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);
        return Collections.emptyList();
    }
}
