package ru.yandex.practicum.exception.model;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}