package ru.yandex.practicum.client;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.stats.proto.ActionTypeProto;
import ru.yandex.practicum.stats.proto.UserActionControllerGrpc;
import ru.yandex.practicum.stats.proto.UserActionProto;
import java.time.Instant;

@Service
@Slf4j
public class CollectorClient {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void collectUserActions(long userId, long eventId, ActionType type) {
        Instant now = Instant.now();
        Timestamp ts = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        ActionTypeProto actionTypeProto = getActionTypeProto(type);
        UserActionProto userActionProto = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionTypeProto)
                .setTimestamp(ts)
                .build();
        try {
            client.collectUserAction(userActionProto);
        } catch (Exception e) {
            log.error("Ошибка при сборе данных о действиях пользователя: {}", e.getMessage());
        }
    }

    private ActionTypeProto getActionTypeProto(ActionType type) {
        return switch (type) {
            case ACTION_LIKE -> ActionTypeProto.ACTION_LIKE;
            case ACTION_VIEW -> ActionTypeProto.ACTION_VIEW;
            case ACTION_REGISTER -> ActionTypeProto.ACTION_REGISTER;
        };
    }
}
