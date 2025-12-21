package ru.yandex.practicum.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "ru.yandex.practicum.interaction.client")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {
        "ru.yandex.practicum.event",
        "ru.practicum.ewm.client.stats"
})
@ConfigurationPropertiesScan
public class EventApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventApplication.class, args);
    }
}