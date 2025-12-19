package ru.yandex.practicum.exception.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.yandex.practicum.exception.model.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class ExceptionApiHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleBadRequestException(BadRequestException e) {
        log.error("BadRequestException: {}", e.getMessage(), e);
        return new ErrorResponse(
                e.getParameter(),
                "Bad request",
                BAD_REQUEST.toString()
        );
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse handleConflictException(ConflictException e) {
        log.error("ConflictException: {}", e.getMessage(), e);
        return new ErrorResponse(
                e.getMessage(),
                "Integrity constraint has been violated.",
                CONFLICT.toString()
        );
    }

    @ExceptionHandler(PublicationException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse handlePublicationException(PublicationException e) {
        log.error("PublicationException: {}", e.getMessage(), e);
        return new ErrorResponse(
                e.getMessage(),
                "Publication failed!",
                CONFLICT.toString()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : "Integrity constraint has been violated.";

        log.error("DataIntegrityViolationException: {}", message, e);

        return new ErrorResponse(
                message,
                "Integrity constraint has been violated.",
                CONFLICT.toString()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException e) {
        log.error("NotFoundException: {}", e.getMessage(), e);
        return new ErrorResponse(
                e.getMessage(),
                "The required object was not found.",
                NOT_FOUND.toString()
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(FORBIDDEN)
    public ErrorResponse handleForbiddenException(ForbiddenException e) {
        log.error("ForbiddenException: {}", e.getMessage(), e);
        return new ErrorResponse(
                e.getMessage(),
                "Access forbidden",
                FORBIDDEN.toString()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format(
                        "Field: %s. Error: %s. Value: %s",
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()
                ))
                .collect(Collectors.toList());

        String message = "Validation failed: " + String.join(", ", errors);

        log.error("MethodArgumentNotValidException: {}", message, e);

        return new ErrorResponse(
                message,
                "Incorrectly made request.",
                BAD_REQUEST.toString()
        );
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class
    })
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleParameterValidationException(Throwable e) {
        log.error("Parameter validation exception: {}", e.getMessage(), e);
        return new ErrorResponse(
                e.getMessage(),
                "Incorrectly made request.",
                BAD_REQUEST.toString()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HttpMessageNotReadableException: {}", e.getMessage(), e);
        return new ErrorResponse(
                "Malformed JSON request. Check the request body.",
                "Incorrectly made request.",
                BAD_REQUEST.toString()
        );
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handleOtherExceptions(Throwable e) {
        log.error("Unexpected error", e);

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        return new ErrorResponse(
                "An unexpected error occurred. Check server logs for details.",
                "Server error",
                INTERNAL_SERVER_ERROR.toString()
        );
    }
}