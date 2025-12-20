package ru.yandex.practicum.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfigProperties {

    private String bootstrapServers;
    private long pollTimeout;

    private Consumer consumer = new Consumer();
    private Topics topics = new Topics();

    @Data
    public static class Consumer {
        private String groupAction;
        private String groupSimilarity;
        private String keyDeserializer;
        private String userActionValueDeserializer;
        private String eventSimilarityValueDeserializer;
        private String autoOffsetReset;
        private boolean enableAutoCommit;
    }

    @Data
    public static class Topics {
        private String statsUserActionV1;
        private String statsEventsSimilarityV1;
    }
}