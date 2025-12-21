package ru.yandex.practicum.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.analyzer.kafka.KafkaProperties;
import ru.yandex.practicum.analyzer.service.UserActionService;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProcessor implements Runnable {

    private final KafkaConsumer<Long, UserActionAvro> userActionsConsumer;
    private final KafkaProperties kafkaProperties;
    private final UserActionService userActionService;
    private final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();


    @Override
    public void run() {

        log.info("UserActionProcessor запущен в потоке: {}", Thread.currentThread().getName());

        Runtime.getRuntime().addShutdownHook(new Thread(userActionsConsumer::wakeup));
        String topic = kafkaProperties.getConsumers().get("user-actions").getTopic();
        Duration pollTimeout = Duration.ofMillis(kafkaProperties.getConsumers().get("user-actions").getPollTimeout());

        try {
            userActionsConsumer.subscribe(List.of(topic));
            log.info("Subscribed to topic: {}", topic);

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = userActionsConsumer.poll(pollTimeout);
                if (!records.isEmpty()) {
                    log.info("Received {} records from Kafka", records.count());
                }

                int count = 0;
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    try {
                        log.debug("Processing record: topic={}, partition={}, offset={}, key={}",
                                record.topic(), record.partition(), record.offset(), record.key());

                        userActionService.handleUserAction(record.value());

                    } catch (Exception e) {
                        log.error("Failed to process record at offset {}: {}",
                                record.offset(), e.getMessage(), e);
                    }

                    currentOffsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );

                    if (++count % 10 == 0) {
                        commitAsyncSafe();
                    }
                }

                commitAsyncSafe();
                currentOffsets.clear();
            }
        } catch (WakeupException ignored) {
            log.info("UserActionProcessor received shutdown signal.");
        } catch (Exception e) {
            log.error("Unexpected error in UserActionProcessor loop", e);
        } finally {
            try {
                userActionsConsumer.commitSync(currentOffsets);
                log.info("Final offsets committed successfully.");
            } finally {
                log.info("Closing Kafka consumer");
                userActionsConsumer.close();
            }
        }
    }

    private void commitAsyncSafe() {
        userActionsConsumer.commitAsync(currentOffsets, (offsets, ex) -> {
            if (ex != null) {
                log.warn("Error committing offsets: {}", offsets, ex);
            }
        });
    }
}