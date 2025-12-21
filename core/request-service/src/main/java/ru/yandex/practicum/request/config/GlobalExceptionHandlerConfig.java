package ru.yandex.practicum.request.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("ru.yandex.practicum.interaction.exception")
public class GlobalExceptionHandlerConfig {
}