package ru.yandex.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service",path = "/admin/users", fallback = UserClientFallback.class)
public interface UserClient extends UserOperations{

}
