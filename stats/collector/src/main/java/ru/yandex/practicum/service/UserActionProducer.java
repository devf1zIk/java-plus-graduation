package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.KafkaConfig;
import ru.yandex.practicum.stats.avro.UserActionAvro;

@Component
@RequiredArgsConstructor
public class UserActionProducer {

    private final KafkaConfig kafka;

    public void sendUserAction(SpecificRecordBase userAction) {
        UserActionAvro action = (UserActionAvro) userAction;
        kafka.send(
                kafka.statsUserActionTopic(),
                String.valueOf(action.getUserId()),
                action.getTimestamp(),
                action
        );
    }
}