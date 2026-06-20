package com.morsel.exception;

import com.morsel.constants.ErrorMessages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_KEY = "correlationId";

    @ExceptionHandler(ApplicationException.class)
    public ProblemDetail handleApplicationException(ApplicationException ex, HttpServletRequest request) {
        log.warn("Application exception [{}]: {}", ex.getStatus(), ex.getMessage());
        return toProblemDetail(ex.getStatus(), ex.getMessage(), ex.getStatus().getReasonPhrase());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationError(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.debug("Validation failure: {}", errors);
        return toProblemDetail(HttpStatus.BAD_REQUEST, errors, ErrorMessages.VALIDATION_FAILURE_TITLE);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        log.debug("Authentication failure: {}", ex.getMessage());
        return toProblemDetail(
                HttpStatus.UNAUTHORIZED, ErrorMessages.INVALID_CREDENTIALS, ErrorMessages.UNAUTHORIZED_TITLE);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return toProblemDetail(HttpStatus.FORBIDDEN, ErrorMessages.ACCESS_DENIED, ErrorMessages.FORBIDDEN_TITLE);
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLocked(LockedException ex) {
        log.debug("Account locked: {}", ex.getMessage());
        return toProblemDetail(
                HttpStatus.TOO_MANY_REQUESTS, ErrorMessages.ACCOUNT_LOCKED, ErrorMessages.TOO_MANY_REQUESTS_TITLE);
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex) {
        log.debug("Account disabled: {}", ex.getMessage());
        return toProblemDetail(HttpStatus.FORBIDDEN, ErrorMessages.ACCOUNT_DISABLED, ErrorMessages.FORBIDDEN_TITLE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedRequest(HttpMessageNotReadableException ex) {
        log.debug("Malformed request: {}", ex.getMessage());
        return toProblemDetail(
                HttpStatus.BAD_REQUEST, ErrorMessages.MALFORMED_REQUEST, ErrorMessages.MALFORMED_REQUEST_TITLE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        log.debug("Missing parameter: {}", ex.getParameterName());
        String detail = "Required parameter '%s' is missing".formatted(ex.getParameterName());
        return toProblemDetail(HttpStatus.BAD_REQUEST, detail, ErrorMessages.INVALID_PARAMETER_TITLE);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.debug("Type mismatch for parameter {}: {}", ex.getName(), ex.getValue());
        String detail = "Parameter '%s' has invalid type".formatted(ex.getName());
        return toProblemDetail(HttpStatus.BAD_REQUEST, detail, ErrorMessages.INVALID_PARAMETER_TITLE);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        log.debug("Constraint violation: {}", errors);
        return toProblemDetail(errors, ErrorMessages.CONSTRAINT_VIOLATION_TITLE);
    }

    @ExceptionHandler(BindException.class)
    public ProblemDetail handleBindException(BindException ex) {
        String errors = ex.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.debug("Binding failure: {}", errors);
        return toProblemDetail(HttpStatus.BAD_REQUEST, errors, ErrorMessages.BINDING_FAILURE_TITLE);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.debug("Upload size exceeded: {}", ex.getMessage());
        return toProblemDetail(
                HttpStatus.PAYLOAD_TOO_LARGE, ErrorMessages.PAYLOAD_TOO_LARGE, ErrorMessages.PAYLOAD_TOO_LARGE_TITLE);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        log.error("Unexpected error [correlationId={}", correlationId, ex);
        return toProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorMessages.UNEXPECTED_ERROR,
                ErrorMessages.INTERNAL_SERVER_ERROR_TITLE);
    }

    private ProblemDetail toProblemDetail(HttpStatusCode status, String detail, String title) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId != null) {
            problem.setProperty(CORRELATION_ID_KEY, correlationId);
        }
        return problem;
    }

    private ProblemDetail toProblemDetail(String detail, String title) {
        return toProblemDetail(HttpStatus.BAD_REQUEST, detail, title);
    }
}
