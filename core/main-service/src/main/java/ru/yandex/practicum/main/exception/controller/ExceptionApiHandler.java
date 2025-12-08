package ru.yandex.practicum.main.exception.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.yandex.practicum.main.exception.model.*;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class ExceptionApiHandler {

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse entityIsAlreadyExist(ConflictException exception) {
        log.warn("Entity is already exist. Message: {}", exception.getMessage(), exception);
        return new ErrorResponse(exception.getMessage(), "Entity is already exist!", CONFLICT.toString());
    }

    @ExceptionHandler(PublicationException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse publicationIsNotExist(PublicationException exception) {
        log.warn("Publication failed. Message: {}", exception.getMessage(), exception);
        return new ErrorResponse(exception.getMessage(), "Publication failed!", CONFLICT.toString());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorResponse entityIsNotExist(NotFoundException exception) {
        log.warn("Entity is not found. Message: {}", exception.getMessage(), exception);
        return new ErrorResponse(exception.getMessage(), "Entity is not found!", NOT_FOUND.toString());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse commonValidation(MethodArgumentNotValidException e) {
        List<FieldError> items = e.getBindingResult().getFieldErrors();
        String message = items.stream()
                .map(FieldError::getField)
                .findFirst()
                .orElse("Unknown error");
        String title = items.stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Unknown error");
        message = message + " - " + title;
        log.warn("Validation error: {}", message, e);

        return new ErrorResponse(message, "Validation error", BAD_REQUEST.toString());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class})
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameterException(final Throwable e) {
        log.warn("MissingServletRequestParameterException. Message: {}", e.getMessage(), e);
        return new ErrorResponse(e.getMessage(), "Validation error", BAD_REQUEST.toString());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handlerMethodValidationException(final Throwable e) {
        log.warn("HandlerMethodValidationException. Message: {}", e.getMessage(), e);
        return new ErrorResponse(e.getMessage(), "Validation error", BAD_REQUEST.toString());
    }

    @ExceptionHandler
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handleOtherExceptions(final Throwable e) {
        log.warn("Exception. Message: {}", e.getMessage(), e);
        return new ErrorResponse(e.getMessage(), "Unknown error", INTERNAL_SERVER_ERROR.toString());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleIncorrectParameterException(final BadRequestException e) {
        log.warn("BadRequestException. Message: {}", e.getMessage(), e);
        return new ErrorResponse(e.getParameter(), "Bad request", BAD_REQUEST.toString());
    }
}