package ru.yandex.practicum.analyzer.service;

import ru.practicum.grpc.stats.messages.RecommendedEventProto;
import java.util.List;

public interface RecommendationService {
    List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults);

    List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults);

    List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds);
}
