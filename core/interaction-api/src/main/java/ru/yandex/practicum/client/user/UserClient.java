package ru.yandex.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import ru.yandex.practicum.config.FeignRetryConfig;

@FeignClient(name = "user-service",path = "/internal/users",configuration = FeignRetryConfig.class, fallback = UserClientFallback.class)
public interface UserClient extends UserOperations{

}
