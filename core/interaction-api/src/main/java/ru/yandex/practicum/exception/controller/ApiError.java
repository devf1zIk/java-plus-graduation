package ru.yandex.practicum.exception.controller;

import lombok.Data;

@Data
public class ApiError {
    String error;

    String status;
    String description;

    public ApiError(String error, String description, String status) {
        this.error = error;
        this.description = description;
        this.status = status;
    }
}