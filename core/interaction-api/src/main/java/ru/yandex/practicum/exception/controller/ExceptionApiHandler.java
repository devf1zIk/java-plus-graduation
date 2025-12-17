package ru.yandex.practicum.exception.controller;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.yandex.practicum.exception.model.*;
import java.util.stream.Collectors;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class ExceptionApiHandler {

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(CONFLICT)
    public ApiError entityIsAlreadyExist(ConflictException exception) {
        log.warn("Entity conflict: {}", exception.getMessage());
        return new ApiError(exception.getMessage(), "Entity conflict", CONFLICT.toString());
    }

    @ExceptionHandler(PublicationException.class)
    @ResponseStatus(CONFLICT)
    public ApiError publicationIsNotExist(PublicationException exception) {
        log.warn("Publication failed: {}", exception.getMessage());
        return new ApiError(exception.getMessage(), "Publication failed", CONFLICT.toString());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ApiError entityIsNotExist(NotFoundException exception) {
        log.warn("Entity not found: {}", exception.getMessage());
        return new ApiError(exception.getMessage(), "Entity not found", NOT_FOUND.toString());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation error: {}", message);
        return new ApiError(message, "Ошибка валидации", BAD_REQUEST.toString());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getMessage());
        return new ApiError(e.getMessage(), "Missing request parameter", BAD_REQUEST.toString());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ApiError handlerMethodValidationException(HandlerMethodValidationException e) {
        log.warn("Method validation error: {}", e.getMessage());
        return new ApiError(e.getMessage(), "Method validation error", BAD_REQUEST.toString());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleIncorrectParameterException(BadRequestException e) {
        log.warn("Bad request: {}", e.getMessage());
        return new ApiError(e.getParameter(), "Bad request", BAD_REQUEST.toString());
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(CONFLICT)
    public ApiError handleAlreadyExists(AlreadyExistsException ex) {
        log.warn("Already exists: {}", ex.getMessage());
        return new ApiError(ex.getMessage(), "Сущность уже существует", CONFLICT.toString());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeignException(FeignException e) {
        log.warn("FeignException. Status: {}, Message: {}", e.status(), e.getMessage());

        if (e.status() == NOT_FOUND.value()) {
            String message = "Ресурс не найден";
            if (e.request() != null && e.request().url().contains("/events/")) {
                message = "Событие не найдено";
            } else if (e.request() != null && e.request().url().contains("/users/")) {
                message = "Пользователь не найден";
            }
            return ResponseEntity.status(NOT_FOUND)
                    .body(new ApiError(message, "Resource not found in external service", NOT_FOUND.toString()));
        }

        if (e.status() >= 500) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Внутренняя ошибка микросервиса", "Internal service error", INTERNAL_SERVER_ERROR.toString()));
        }

        return ResponseEntity.status(SERVICE_UNAVAILABLE)
                    .body(new ApiError("Ошибка при вызове микросервиса", "Service unavailable", SERVICE_UNAVAILABLE.toString()));
    }

    @ExceptionHandler(feign.FeignException.ServiceUnavailable.class)
    @ResponseStatus(SERVICE_UNAVAILABLE)
    public ApiError handleFeignServiceUnavailable(feign.FeignException.ServiceUnavailable e) {
        log.error("Микросервис недоступен: {}", e.getMessage());
        return new ApiError("Микросервис временно недоступен", "Service unavailable", SERVICE_UNAVAILABLE.toString());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ApiError handleOtherExceptions(Throwable e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return new ApiError(e.getMessage(), "Internal server error", INTERNAL_SERVER_ERROR.toString());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(cv -> {
                    String property = cv.getPropertyPath().toString();
                    String paramName = property.substring(property.lastIndexOf('.') + 1);
                    return paramName + ": " + cv.getMessage();
                })
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", message);
        return new ApiError(message, "Ошибка валидации параметров", BAD_REQUEST.toString());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("Параметр '%s' должен быть числом", e.getName());
        log.warn("Type mismatch: {}", message);
        return new ApiError(message, "Некорректный тип параметра", BAD_REQUEST.toString());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(BAD_REQUEST)
    public ApiError handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request due to invalid argument: {}", e.getMessage());
        return new ApiError(e.getMessage(), "Некорректные параметры запроса", BAD_REQUEST.toString());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(SERVICE_UNAVAILABLE)
    public ApiError handleServiceUnavailable(ServiceUnavailableException e) {
        log.error("Service unavailable: {}", e.getMessage());
        return new ApiError(e.getMessage(), "Service unavailable", SERVICE_UNAVAILABLE.toString());
    }
}