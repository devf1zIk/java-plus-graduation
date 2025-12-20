package ru.yandex.practicum.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import java.time.Duration;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaConfigProperties properties;

    private Producer<Long, SpecificRecordBase> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.getProducer().getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                properties.getProducer().getKeySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                properties.getProducer().getValueSerializer());
        return new KafkaProducer<>(props);
    }

    private Consumer<Long, SpecificRecordBase> createConsumer(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.getConsumer().getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                properties.getConsumer().getKeyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                properties.getConsumer().getValueDeserializer());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    @Bean
    @Scope("prototype")
    public KafkaClient kafkaClient() {
        return new KafkaClient() {

            private Consumer<Long, SpecificRecordBase> consumer;
            private Producer<Long, SpecificRecordBase> producer;

            @Override
            public Consumer<Long, SpecificRecordBase> getConsumer() {
                if (consumer == null) {
                    consumer = createConsumer(properties.getConsumer().getGroupId());
                }
                return consumer;
            }

            @Override
            public Producer<Long, SpecificRecordBase> getProducer() {
                if (producer == null) {
                    producer = createProducer();
                }
                return producer;
            }

            @Override
            public Duration getPollTimeout() {
                return properties.getPollTimeout();
            }

            @Override
            public KafkaConfigProperties.Topics getTopics() {
                return properties.getTopics();
            }
        };
    }
}
