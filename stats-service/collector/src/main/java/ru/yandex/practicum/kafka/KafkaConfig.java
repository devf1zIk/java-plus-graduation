package ru.yandex.practicum.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@Slf4j
public class KafkaConfig {

    private final KafkaProducer<String, SpecificRecordBase> producer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaConfigProperties properties;

    public KafkaConfig(KafkaConfigProperties properties) {
        this.properties = properties;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientIdConfig());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, properties.getProducerKeySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, properties.getProducerValueSerializer());

        this.producer = new KafkaProducer<>(props);
    }

    public void send(String topic, String key, Instant timestamp, SpecificRecordBase value) {
        long ts = timestamp.toEpochMilli();
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, null, ts, key, value);

        producer.send(record, (metadata, exception) -> {
            try {
                Map<String, Object> logData = new HashMap<>();
                logData.put("topic", topic);

                if (exception != null) {
                    logData.put("event", "kafka_send_failed");
                    logData.put("error", exception.toString());
                    log.error(objectMapper.writeValueAsString(logData));
                } else {
                    logData.put("event", "kafka_send_success");
                    logData.put("partition", metadata.partition());
                    logData.put("offset", metadata.offset());
                    logData.put("producer_timestamp", ts);
                    logData.put("kafka_timestamp", metadata.timestamp());
                    log.info(objectMapper.writeValueAsString(logData));
                }
            } catch (Exception e) {
                log.error("Kafka log serialization error", e);
            }
        });
    }

    @PreDestroy
    public void close() {
        producer.close();
    }

    public String statsUserActionTopic() {
        return properties.getStatsUserActionV1();
    }
}