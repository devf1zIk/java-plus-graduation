package ru.yandex.practicum.interaction.exception;


import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;


@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            if (response.body() != null) {
                String body = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
                log.error("Feign ошибка при вызове {}: {}", methodKey, body);

                String exceptionName = extractField(body, "\"exception\":\"", "\"");
                String reason = extractField(body, "\"reason\":\"", "\"");

                if (exceptionName != null) {
                    switch (exceptionName) {
                        case "NoSuchElementException":
                            return new NoSuchElementException(reason);
                        case "IllegalStateException":
                            return new IllegalStateException(reason);
                        case "ValidationException":
                            return new ValidationException(reason);
                        case "NotFoundException":
                            return new NotFoundException(reason);
                        case "IllegalArgumentException":
                            return new IllegalArgumentException(reason);
                        case "ConflictException":
                            return new ConflictException(reason);
                        default:
                            log.warn("Неизвестная ошибка по reason: {}, возвращаю RuntimeException", exceptionName);
                            return new RuntimeException("Ошибка: " + (reason != null ? reason : "Unknown error"));
                    }
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при чтении тела ответа Feign", e);
            return new RuntimeException("Ошибка при обработке ответа Feign", e);
        }
        return defaultDecoder.decode(methodKey, response);
    }

    private String extractField(String body, String start, String end) {
        int startIndex = body.indexOf(start);
        if (startIndex == -1) return null;
        int endIndex = body.indexOf(end, startIndex + start.length());
        if (endIndex == -1) return null;
        return body.substring(startIndex + start.length(), endIndex);
    }
}