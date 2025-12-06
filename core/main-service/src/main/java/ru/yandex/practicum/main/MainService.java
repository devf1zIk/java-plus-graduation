package ru.yandex.practicum.main;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"ru.yandex.practicum.client",
        "ru.yandex.practicum.main.category",
        "ru.yandex.practicum.main.event",
        "ru.yandex.practicum.main.category",
        "ru.yandex.practicum.main.exception",
        "ru.yandex.practicum.main.request",
        "ru.yandex.practicum.main.compilation",
        "ru.yandex.practicum.main.user",
        "ru.yandex.practicum.main.comment"})
public class MainService {
    public static void main(String[] args) {
        SpringApplication.run(MainService.class, args);
    }

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // Настройки модуля (опционально)
        module.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        module.disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
        return module;
    }
}
