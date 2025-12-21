package ru.practicum.ewm.server.exception;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(final Exception e) {
        log.error("Error 500 {}", e.getMessage(), e);
        return buildApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", e);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingRequestParam(final MissingServletRequestParameterException e) {
        log.error("Error 400 (MissingServletRequestParameterException): {}", e.getMessage(), e);
        return buildApiError(HttpStatus.BAD_REQUEST,
                String.format("Missing required parameter: %s", e.getParameterName()), e);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(final ValidationException e) {
        log.error("Error 400 (ValidationException): {}", e.getMessage(), e);
        return buildApiError(HttpStatus.BAD_REQUEST, "Validation error", e);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(final IllegalArgumentException e) {
        log.error("Error 400 (IllegalArgument): {}", e.getMessage(), e);
        return buildApiError(HttpStatus.BAD_REQUEST, "Invalid query parameters", e);
    }

    private ApiError buildApiError(HttpStatus status, String reason, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return new ApiError(status, reason, e.getMessage(), sw.toString());
    }
}