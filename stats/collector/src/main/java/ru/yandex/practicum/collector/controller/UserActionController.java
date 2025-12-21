package ru.yandex.practicum.collector.controller;

import ru.yandex.practicum.collector.kafka.KafkaProducerService;
import ru.yandex.practicum.collector.kafka.KafkaProperties;
import ru.yandex.practicum.collector.mapper.UserActionMapper;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;
import ru.practicum.grpc.stats.messages.UserActionProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaProducerService kafkaProducerService;
    private final KafkaProperties kafkaProperties;
    private final UserActionMapper mapper;

    @Override
    public void collectUserAction(UserActionProto request,
                                  StreamObserver<Empty> responseObserver) {
        try {
            log.info("New gRPC request received: CollectUserAction");
            log.debug("Request data: userId={}, eventId={}, actionType={}, timestamp={}",
                    request.getUserId(),
                    request.getEventId(),
                    request.getActionType(),
                    request.getTimestamp());


            UserActionAvro avroMessage = mapper.toAvro(request);

            log.info("Converted request to Avro message: {}", avroMessage);

            String topic = kafkaProperties.getProducer().getTopic();
            Long key = request.getUserId(); // key = user id


            kafkaProducerService.send(topic, key, avroMessage);

            log.info("Message successfully sent to Kafka [topic={}, userId={}, eventId={}]",
                    topic, request.getUserId(), request.getEventId());


            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
            log.info("gRPC response sent: SUCCESS");
        } catch (Exception e) {
            log.error("Error while processing user action: {}", e.getMessage(), e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to process user action")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}
