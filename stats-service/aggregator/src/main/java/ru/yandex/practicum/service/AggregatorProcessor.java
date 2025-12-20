package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.KafkaClient;
import ru.yandex.practicum.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.stats.avro.UserActionAvro;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregatorProcessor {

    private final KafkaClient kafkaClient;
    private final SimilarityCalculator calculator;

    public void start() {
        org.apache.kafka.clients.consumer.Consumer<Long, SpecificRecordBase> consumer =
                kafkaClient.getConsumer();
        Producer<Long, SpecificRecordBase> producer =
                kafkaClient.getProducer();

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(
                    List.of(kafkaClient.getTopics().getStatsUserActionV1())
            );

            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records =
                        consumer.poll(kafkaClient.getPollTimeout());

                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    handleRecord(record, producer);
                }

                consumer.commitAsync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка обработки user-action", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync();
            } finally {
                consumer.close();
                producer.close();
            }
        }
    }

    private void handleRecord(
            ConsumerRecord<Long, SpecificRecordBase> record,
            Producer<Long, SpecificRecordBase> producer
    ) {
        if (!(record.value() instanceof UserActionAvro action)) {
            log.warn("Неизвестное сообщение {}", record.value().getClass());
            return;
        }

        List<EventSimilarityAvro> similarities =
                calculator.calculateSimilarity(action);

        for (EventSimilarityAvro similarity : similarities) {
            producer.send(new ProducerRecord<>(
                    kafkaClient.getTopics().getStatsEventsSimilarityV1(),
                    similarity.getEventA(),
                    similarity
            ));
        }
    }
}
