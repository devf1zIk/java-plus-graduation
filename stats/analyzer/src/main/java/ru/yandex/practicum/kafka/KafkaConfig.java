package ru.yandex.practicum.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import java.time.Duration;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaConfigProperties properties;

    private Consumer<Long, SpecificRecordBase> createConsumer(
            String groupId,
            String valueDeserializer
    ) {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                properties.getConsumer().getKeyDeserializer());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                properties.getConsumer().getAutoOffsetReset());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                properties.getConsumer().isEnableAutoCommit());

        return new KafkaConsumer<>(config);
    }

    @Bean
    @Scope("prototype")
    public KafkaClient kafkaClient() {
        return new KafkaClient() {

            private Consumer<Long, SpecificRecordBase> actionConsumer;
            private Consumer<Long, SpecificRecordBase> similarityConsumer;

            @Override
            public Consumer<Long, SpecificRecordBase> getConsumerAction() {
                if (actionConsumer == null) {
                    actionConsumer = createConsumer(
                            properties.getConsumer().getGroupAction(),
                            properties.getConsumer().getUserActionValueDeserializer()
                    );
                }
                return actionConsumer;
            }

            @Override
            public Consumer<Long, SpecificRecordBase> getConsumerSimilarity() {
                if (similarityConsumer == null) {
                    similarityConsumer = createConsumer(
                            properties.getConsumer().getGroupSimilarity(),
                            properties.getConsumer().getEventSimilarityValueDeserializer()
                    );
                }
                return similarityConsumer;
            }

            @Override
            public Duration getPollTimeout() {
                return Duration.ofMillis(properties.getPollTimeout());
            }

            @Override
            public KafkaConfigProperties.Topics getTopics() {
                return properties.getTopics();
            }
        };
    }
}
