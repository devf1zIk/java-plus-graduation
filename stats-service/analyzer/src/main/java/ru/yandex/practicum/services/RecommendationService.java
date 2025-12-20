package ru.yandex.practicum.services;

import ru.yandex.practicum.stats.proto.InteractionsCountRequestProto;
import ru.yandex.practicum.stats.proto.RecommendedEventProto;
import ru.yandex.practicum.stats.proto.SimilarEventsRequestProto;
import ru.yandex.practicum.stats.proto.UserPredictionsRequestProto;
import java.util.stream.Stream;

public interface RecommendationService {

    Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request);

    Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request);

    Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request);
}
