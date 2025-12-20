package ru.yandex.practicum.kafka.telemetry.deserialization;

public class DeserializationException extends RuntimeException {
    public DeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}