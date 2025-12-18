package ru.yandex.practicum.exception.controller;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.yandex.practicum.exception.model.*;
import java.util.List;
import java.util.stream.Collectors;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class ExceptionApiHandler {


    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleBadRequestException(final BadRequestException e) {
        log.warn("BadRequestException: {}", e.getMessage());
        return new ErrorResponse(e.getParameter(), "Bad request", BAD_REQUEST.toString());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse handleConflictException(ConflictException exception) {
        log.warn("ConflictException: {}", exception.getMessage());
        return new ErrorResponse(exception.getMessage(), "Integrity constraint has been violated.", CONFLICT.toString());
    }

    @ExceptionHandler(PublicationException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse handlePublicationException(PublicationException exception) {
        log.warn("PublicationException: {}", exception.getMessage());
        return new ErrorResponse(exception.getMessage(), "Publication failed!", CONFLICT.toString());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        String message = "Integrity constraint has been violated.";
        if (e.getMostSpecificCause() != null) {
            message = e.getMostSpecificCause().getMessage();
        }
        log.warn("DataIntegrityViolationException: {}", message);
        return new ErrorResponse(message, "Integrity constraint has been violated.", CONFLICT.toString());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException exception) {
        log.warn("NotFoundException: {}", exception.getMessage());
        return new ErrorResponse(exception.getMessage(), "The required object was not found.", NOT_FOUND.toString());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(FORBIDDEN)
    public ErrorResponse handleForbiddenException(ForbiddenException exception) {
        log.warn("ForbiddenException: {}", exception.getMessage());
        return new ErrorResponse(exception.getMessage(), "Access forbidden", FORBIDDEN.toString());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .collect(Collectors.toList());

        String message = "Validation failed: " + String.join(", ", errors);
        log.warn("MethodArgumentNotValidException: {}", message);

        return new ErrorResponse(message, "Incorrectly made request.", BAD_REQUEST.toString());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, HandlerMethodValidationException.class})
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleParameterValidationException(final Throwable e) {
        log.warn("Parameter validation exception: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), "Incorrectly made request.", BAD_REQUEST.toString());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        String message = "Malformed JSON request. Check the request body.";
        return new ErrorResponse(message, "Incorrectly made request.", BAD_REQUEST.toString());
    }

    @ExceptionHandler(FeignException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handleFeignException(FeignException e) {
        log.error("Feign client error: status {}, message {}", e.status(), e.getMessage());
        return new ErrorResponse("Error communicating with another service.", "Service communication error", INTERNAL_SERVER_ERROR.toString());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handleOtherExceptions(final Throwable e) {
        log.error("Unexpected error: ", e);
        return new ErrorResponse("An unexpected error occurred.", "Server error", INTERNAL_SERVER_ERROR.toString());
    }
}