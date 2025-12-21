package ru.yandex.practicum.collector.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final Producer<Long, SpecificRecordBase> producer;

    public void send(String topic, Long key, UserActionAvro message) {
        ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(topic, key, message);
        producer.send(record);
    }
}
