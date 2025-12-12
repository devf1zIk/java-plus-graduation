package ru.yandex.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service", contextId = "userAdminClient", fallback = UserClientFallback.class)
public interface UserClient extends UserOperations {

}
