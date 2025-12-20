package ru.yandex.practicum.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaConfigProperties {

    private Producer producer;
    private Consumer consumer;
    private Topics topics;
    private Duration pollTimeout;

    @Data
    public static class Producer {
        private String bootstrapServers;
        private String keySerializer;
        private String valueSerializer;
    }

    @Data
    public static class Consumer {
        private String bootstrapServers;
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
    }

    @Data
    public static class Topics {
        private String statsUserActionV1;
        private String statsEventsSimilarityV1;
    }
}