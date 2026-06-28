package com.morsel.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        MDC.put("correlationId", "test-corr-id");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("handles DuplicateResourceException as 409 Conflict")
    void handleApplicationException_returnsProblemDetailWithCorrectStatus() {
        DuplicateResourceException ex = new DuplicateResourceException("Username already exists");

        ProblemDetail result = handler.handleApplicationException(ex);

        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getDetail()).isEqualTo("Username already exists");
    }

    @Test
    @DisplayName("handles ResourceNotFoundException as 404 Not Found")
    void handleApplicationException_withNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");

        ProblemDetail result = handler.handleApplicationException(ex);

        assertThat(result.getStatus()).isEqualTo(404);
        assertThat(result.getDetail()).isEqualTo("User not found");
    }

    @Test
    @DisplayName("handles AccessDeniedException as 403 Forbidden")
    void handleAccessDenied_returns403() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ProblemDetail result = handler.handleAccessDenied(ex);

        assertThat(result.getStatus()).isEqualTo(403);
        assertThat(result.getTitle()).isEqualTo("Forbidden");
        assertThat(result.getDetail()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("handles LockedException as 429 Too Many Requests")
    void handleLocked_returns429() {
        LockedException ex = new LockedException("Account is locked");

        ProblemDetail result = handler.handleLocked(ex);

        assertThat(result.getStatus()).isEqualTo(429);
        assertThat(result.getTitle()).isEqualTo("Too Many Requests");
        assertThat(result.getDetail()).contains("locked");
    }

    @Test
    @DisplayName("handles DisabledException as 403 Forbidden")
    void handleDisabled_returns403() {
        DisabledException ex = new DisabledException("Account is disabled");

        ProblemDetail result = handler.handleDisabled(ex);

        assertThat(result.getStatus()).isEqualTo(403);
        assertThat(result.getTitle()).isEqualTo("Forbidden");
        assertThat(result.getDetail()).contains("disabled");
    }

    @Test
    @DisplayName("handles generic Exception as 500 Internal Server Error")
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("Something broke");

        ProblemDetail result = handler.handleGeneric(ex);

        assertThat(result.getStatus()).isEqualTo(500);
        assertThat(result.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("handles ConstraintViolationException as 400 Bad Request")
    void handleConstraintViolation_returns400() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("age");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be positive");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ProblemDetail result = handler.handleConstraintViolation(ex);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getTitle()).isEqualTo("Validation Failure");
        assertThat(result.getDetail()).contains("must be positive");
    }

    @Test
    @DisplayName("includes correlationId in ProblemDetail when MDC is set")
    void handleGenericException_includesCorrelationId() {
        Exception ex = new RuntimeException("Something broke");

        ProblemDetail result = handler.handleGeneric(ex);

        assertThat(result.getProperties()).containsEntry("correlationId", "test-corr-id");
    }

    @Test
    @DisplayName("handles UnauthorizedException as 401 Unauthorized")
    void handleUnauthorizedException_returns401() {
        UnauthorizedException ex = new UnauthorizedException("Invalid or expired refresh token");

        ProblemDetail result = handler.handleApplicationException(ex);

        assertThat(result.getStatus()).isEqualTo(401);
        assertThat(result.getDetail()).isEqualTo("Invalid or expired refresh token");
    }
}
