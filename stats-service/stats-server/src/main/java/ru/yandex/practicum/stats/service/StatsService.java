package ru.yandex.practicum.stats.service;

import ru.yandex.practicum.dto.HitDto;
import ru.yandex.practicum.dto.StatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    void hit(HitDto hitDto);

    List<StatsDto> getStats(LocalDateTime startRange, LocalDateTime endRange, List<String> uris, boolean unique);
}
