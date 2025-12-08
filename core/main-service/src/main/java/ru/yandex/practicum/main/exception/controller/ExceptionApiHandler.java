package ru.yandex.practicum.main.exception.controller;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.yandex.practicum.main.exception.model.*;
import java.util.List;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class ExceptionApiHandler {

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse entityIsAlreadyExist(ConflictException ex) {
        log.warn("ConflictException: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage(), "Entity already exists", CONFLICT.toString());
    }

    @ExceptionHandler(PublicationException.class)
    @ResponseStatus(CONFLICT)
    public ErrorResponse publicationIsNotExist(PublicationException ex) {
        log.warn("PublicationException: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage(), "Publication failed", CONFLICT.toString());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorResponse entityIsNotExist(NotFoundException ex) {
        log.warn("NotFoundException: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage(), "Entity not found", NOT_FOUND.toString());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors();
        String field = errors.stream().map(FieldError::getField).findFirst().orElse("unknown");
        String msg = errors.stream().map(FieldError::getDefaultMessage).findFirst().orElse("validation error");
        String message = field + " - " + msg;
        log.warn("Validation error: {}", message, ex);
        return new ErrorResponse(message, "Validation error", BAD_REQUEST.toString());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing request param: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage(), "Validation error", BAD_REQUEST.toString());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        log.warn("Handler method validation failed: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage(), "Validation error", BAD_REQUEST.toString());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException ex) {
        log.warn("BadRequestException: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getParameter(), "Bad request", BAD_REQUEST.toString());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage(), "Validation error", BAD_REQUEST.toString());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: {} should be {}", ex.getName(), ex.getRequiredType(), ex);
        return new ErrorResponse(
                "Parameter '" + ex.getName() + "' is invalid",
                "Validation error",
                BAD_REQUEST.toString()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON: {}", ex.getMessage(), ex);
        return new ErrorResponse("Malformed JSON", "Validation error", BAD_REQUEST.toString());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ErrorResponse handleOtherExceptions(Throwable ex) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage(), "Unknown error", INTERNAL_SERVER_ERROR.toString());
    }
}