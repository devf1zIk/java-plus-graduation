package ru.practicum;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication(scanBasePackages = {"ru.practicum.client",
        "ru.practicum.category",
        "ru.practicum.event",
        "ru.practicum.category",
        "ru.practicum.exception",
        "ru.practicum.request",
        "ru.practicum.compilation",
        "ru.practicum.user",
        "ru.practicum.comment"})
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
