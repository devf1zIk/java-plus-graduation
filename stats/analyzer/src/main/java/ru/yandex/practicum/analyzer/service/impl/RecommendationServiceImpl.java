package ru.yandex.practicum.analyzer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import ru.practicum.grpc.stats.messages.RecommendedEventProto;
import ru.yandex.practicum.analyzer.model.EventSimilarity;
import ru.yandex.practicum.analyzer.model.UserEventWeight;
import ru.yandex.practicum.analyzer.repository.EventSimilarityRepository;
import ru.yandex.practicum.analyzer.repository.UserEventWeightRepository;
import ru.yandex.practicum.analyzer.service.RecommendationService;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final UserEventWeightRepository userEventRepository;
    private final EventSimilarityRepository similarityRepository;

    private static final int RECENT_WINDOW = 20;

    private static final int NEIGHBORHOOD_SIZE = 20;

    @Override
    public List<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        log.info("Calculating similar events for eventId={} and userId={}", eventId, userId);

        List<EventSimilarity> similarities = similarityRepository.findByEvent(eventId);

        List<Long> interactedEvents = userEventRepository.findEventIdsByUserId(userId);

        return similarities.stream()
                .filter(sim -> !interactedEvents.contains(sim.getOtherEventId(eventId)))
                .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
                .limit(maxResults)
                .map(sim -> RecommendedEventProto.newBuilder()
                        .setEventId(sim.getOtherEventId(eventId))
                        .setScore(sim.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.info("Generating recommendations with predicted scores for userId={}", userId);

        Pageable recentPage = PageRequest.of(0, RECENT_WINDOW, Sort.by("eventTime").descending());
        List<UserEventWeight> recent = userEventRepository.findRecentByUserId(userId, recentPage);
        if (recent.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> seenEventIds = recent.stream()
                .map(UserEventWeight::getEventId)
                .distinct()
                .toList();

        List<Long> alreadyInteracted = userEventRepository.findEventIdsByUserId(userId);

        List<Pair<Long, EventSimilarity>> candidates = seenEventIds.stream()
                .flatMap(seedId ->
                        similarityRepository.findByEvent(seedId).stream()
                                .map(sim -> Pair.of(seedId, sim))
                )
                .filter(pair -> {
                    long seedId = pair.getFirst();
                    long target = pair.getSecond().getOtherEventId(seedId);
                    return !alreadyInteracted.contains(target);
                })
                .toList();

        Map<Long, List<Pair<Long, EventSimilarity>>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(pair ->
                        pair.getSecond().getOtherEventId(pair.getFirst())
                ));

        Map<Long, Double> knownWeights = recent.stream()
                .collect(Collectors.toMap(UserEventWeight::getEventId, UserEventWeight::getWeight));

        List<RecommendedEventProto> predictions = new ArrayList<>();
        for (Map.Entry<Long, List<Pair<Long, EventSimilarity>>> entry : grouped.entrySet()) {
            Long newEventId = entry.getKey();

            List<EventSimilarity> neighbors = entry.getValue().stream()
                    .map(Pair::getSecond)
                    .sorted(Comparator.comparingDouble(EventSimilarity::getScore).reversed())
                    .limit(NEIGHBORHOOD_SIZE)
                    .toList();

            double numerator = 0.0, denominator = 0.0;
            for (EventSimilarity sim : neighbors) {
                long knownEventId = sim.getOtherEventId(newEventId);
                Double weight = knownWeights.get(knownEventId);
                if (weight == null) {
                    continue;
                }
                numerator += weight * sim.getScore();
                denominator += sim.getScore();
            }
            if (denominator == 0.0) {
                continue;
            }

            double predictedScore = numerator / denominator;
            predictions.add(RecommendedEventProto.newBuilder()
                    .setEventId(newEventId)
                    .setScore(predictedScore)
                    .build());
        }

        return predictions.stream()
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .limit(maxResults)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.info("Calculating interaction counts for events={}", eventIds);

        return eventIds.stream()
                .map(eventId -> {
                    Double sum = userEventRepository.sumWeightsByEventId(eventId);
                    double sumWeights = sum != null ? sum : 0.0;
                    return RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(sumWeights)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
