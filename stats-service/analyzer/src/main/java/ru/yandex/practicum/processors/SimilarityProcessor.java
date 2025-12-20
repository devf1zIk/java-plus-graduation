package ru.yandex.practicum.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.handler.SimilarityHandler;
import ru.yandex.practicum.kafka.KafkaClient;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityProcessor implements Runnable {

    private final KafkaClient kafkaClient;
    private final SimilarityHandler handler;

    private final Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();

    @Override
    public void run() {
        Consumer<Long, SpecificRecordBase> consumer =
                kafkaClient.getConsumerSimilarity();

        try {
            consumer.subscribe(
                    List.of(kafkaClient.getTopics().getStatsEventsSimilarityV1())
            );

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<Long, SpecificRecordBase> records =
                        consumer.poll(kafkaClient.getPollTimeout());

                int i = 0;
                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    if (record.value() instanceof EventSimilarityAvro avro) {
                        handler.handle(avro);
                    }
                    trackOffset(record);
                    if (++i % 10 == 0) {
                        commitAsync(consumer);
                    }
                }
            }
        } catch (WakeupException ignored) {
        } finally {
            commitSync(consumer);
            consumer.close();
        }
    }

    private void trackOffset(ConsumerRecord<?, ?> record) {
        offsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );
    }

    private void commitAsync(Consumer<?, ?> consumer) {
        consumer.commitAsync(offsets, null);
    }

    private void commitSync(Consumer<?, ?> consumer) {
        consumer.commitSync(offsets);
    }
}
