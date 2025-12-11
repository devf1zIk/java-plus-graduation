package ru.yandex.practicum.client.request;

import java.util.List;
import java.util.Map;

public interface RequestOperations {

    Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds);
}
