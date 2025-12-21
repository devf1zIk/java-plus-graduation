package ru.yandex.practicum.interaction.fallback;


import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.interaction.client.EventClient;
import ru.yandex.practicum.interaction.dto.EventFullDto;

@Slf4j
public class EventFallback implements EventClient {

    @Override
    public EventFullDto getEventForInternalUse(Long id) {
        log.warn("[EventClient#getEventForInternalUse] Fallback triggered: get event by ID {} failed.", id);
        return null;
    }
}
