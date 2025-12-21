package ru.yandex.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.aggregator.kafka.KafkaProperties;
import ru.yandex.practicum.aggregator.producer.AggregatorKafkaProducer;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {

    private final AggregatorKafkaProducer kafkaProducer;
    private final KafkaProperties kafkaProperties;

    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();

    private final Map<Long, Map<Long, Double>> similarities = new HashMap<>();

    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    private final Map<Long, Double> eventSums = new HashMap<>();

    public void processUserAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double newWeight = getActionWeight(action);

        log.info("[Aggregator] Received user action: userId={}, eventId={}, type={}, weight={}",
                userId, eventId, action.getActionType(), newWeight);


        boolean isNewEvent = !userEventWeights.containsKey(eventId);

        if (isNewEvent) {
            handleNewEvent(eventId, userId, newWeight);
        } else {
            handleExistingEvent(eventId, userId, newWeight);
        }
    }

    private void handleNewEvent(long eventId, long userId, double newWeight) {
        log.info("[Aggregator] Detected NEW event: eventId={}", eventId);

        Map<Long, Double> userWeights = new HashMap<>();
        userWeights.put(userId, newWeight);
        userEventWeights.put(eventId, userWeights);
        eventSums.put(eventId, newWeight);

        for (Long otherEventId : userEventWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            if (isOtherWeightZero(userId, otherEventId)) continue;

            double S_min = getSMin(eventId, otherEventId);
            saveSMinToMemory(eventId, otherEventId, S_min);

            double sumA = eventSums.get(eventId);
            double sumB = eventSums.get(otherEventId);

            double similarity = S_min / Math.sqrt(sumA * sumB);

            log.info("[Aggregator] Calculated similarity with new event: eventA={}, eventB={}, similarity={}",
                    eventId, otherEventId, similarity);

            saveAndSendSimilarity(eventId, otherEventId, similarity);
        }
    }

    private void saveSMinToMemory(long eventId, Long otherEventId, double S_min) {
        long first = Math.min(eventId, otherEventId);
        long second = Math.max(eventId, otherEventId);

        minWeightsSums
                .computeIfAbsent(first, f -> new HashMap<>())
                .put(second, S_min);

        log.info("[Aggregator] Updated minWeightsSums matrix: eventA={}, eventB={}, S_min={}",
                first, second, S_min);
    }


    private double getSMin(long eventId, Long otherEventId) {
        var allUsers = new HashSet<Long>();
        allUsers.addAll(userEventWeights.get(eventId).keySet());
        allUsers.addAll(userEventWeights.get(otherEventId).keySet());

        double S_min = 0.0;
        for (Long u : allUsers) {
            double wA = userEventWeights.get(eventId).getOrDefault(u, 0.0);
            double wB = userEventWeights.get(otherEventId).getOrDefault(u, 0.0);
            S_min += Math.min(wA, wB);
        }
        return S_min;
    }

    private boolean isOtherWeightZero(long userId, Long otherEventId) {
        double otherWeight = userEventWeights.get(otherEventId).getOrDefault(userId, 0.0);
        if (otherWeight == 0.0) {
            log.debug("[Aggregator] Skipped similarity: userId={} has no interaction with eventId={}",
                    userId, otherEventId);
            return true;
        }
        return false;
    }

    private void handleExistingEvent(long eventId, long userId, double newWeight) {

        Map<Long, Double> userWeights = userEventWeights.get(eventId); //userId -> weight
        double oldWeight = userWeights.getOrDefault(userId, 0.0);

        if (newWeight <= oldWeight) {
            log.debug("[Aggregator] Skipped update: new weight ({}) <= old weight ({}) for userId={} and eventId={}",
                    newWeight, oldWeight, userId, eventId);
            return;
        }

        userWeights.put(userId, newWeight);

        log.info("[Aggregator] Updated user weight for existing event: eventId={}, userId={}, oldWeight={}, newWeight={}",
                eventId, userId, oldWeight, userEventWeights.get(eventId).get(userId));

        double newSumA = getAndSaveSumANew(eventId, newWeight - oldWeight);

        for (Long otherEventId : userEventWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            if (isOtherWeightZero(userId, otherEventId)) continue;

            double otherWeight = userEventWeights.get(otherEventId).get(userId);

            double minOld = Math.min(oldWeight, otherWeight);
            double minNew = Math.min(newWeight, otherWeight);
            double minDelta = minNew - minOld;

            double S_min_new = getAndSaveSMinNew(eventId, otherEventId, minDelta);

            double sumB = eventSums.get(otherEventId);

            double similarity = S_min_new / Math.sqrt(newSumA * sumB);

            log.info("[Aggregator] Calculated similarity: eventA={}, eventB={}, similarity={}",
                    eventId, otherEventId, similarity);

            saveAndSendSimilarity(eventId, otherEventId, similarity);
        }
    }

    private double getAndSaveSumANew(long eventId, double deltaWeight) {

        double sumA_old = eventSums.get(eventId);
        double sumA_new = sumA_old + deltaWeight;
        eventSums.put(eventId, sumA_new);
        log.debug("[Aggregator] Updated sumA: eventId={}, sumA_old={}, sumA_new={}",
                eventId, sumA_old, sumA_new);
        return sumA_new;
    }

    private double getAndSaveSMinNew(long eventId, Long otherEventId, double minDelta) {
        long first = Math.min(eventId, otherEventId);
        long second = Math.max(eventId, otherEventId);

        double S_min_old = minWeightsSums
                .computeIfAbsent(first, k -> new HashMap<>())
                .getOrDefault(second, 0.0);
        double S_min_new = S_min_old + minDelta;

        if (S_min_new > S_min_old) {
            minWeightsSums
                    .computeIfAbsent(first, k -> new HashMap<>())
                    .put(second, S_min_new);
            log.debug("[Aggregator] Updated S_min: firstEvent={}, secondEvent={}, oldSum={}, newSum={}",
                    first, second, S_min_old, S_min_new);
        }
        return S_min_new;
    }

    private void saveAndSendSimilarity(long eventA, long eventB, double similarity) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        similarities
                .computeIfAbsent(first, f -> new HashMap<>())
                .put(second, similarity);

        log.info("[Aggregator] Updated similarity matrix: eventA={}, eventB={}, similarity={}",
                first, second, similarity);

        sendSimilarity(first, second, similarity);
    }

    private void sendSimilarity(long first, long second, double similarity) {
        EventSimilarityAvro similarityAvro = EventSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(similarity)
                .setTimestamp(Instant.ofEpochMilli(Instant.now().toEpochMilli()))
                .build();

        kafkaProducer.send(kafkaProperties.getProducer().getTopic(), first, similarityAvro);

        log.info("[Aggregator] Sent similarity to Kafka: eventA={}, eventB={}, score={}", first, second, similarity);
    }

    private double getActionWeight(UserActionAvro action) {
        return switch (action.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}
