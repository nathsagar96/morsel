package com.morsel.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
    @DisplayName("handles BadCredentialsException as 401 Unauthorized")
    void handleBadCredentials_returns401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ProblemDetail result = handler.handleBadCredentials(ex);

        assertThat(result.getStatus()).isEqualTo(401);
        assertThat(result.getTitle()).isEqualTo("Unauthorized");
        assertThat(result.getDetail()).isEqualTo("Invalid username or password");
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
    @DisplayName("handles generic Exception as 500 Internal Server Error")
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("Something broke");

        ProblemDetail result = handler.handleGeneric(ex);

        assertThat(result.getStatus()).isEqualTo(500);
        assertThat(result.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("handles validation errors as 400 with field details")
    void handleValidationError_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = createValidationException("username", "must not be blank");

        ProblemDetail result = handler.handleValidationError(ex);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getDetail()).contains("username", "must not be blank");
    }

    private MethodArgumentNotValidException createValidationException(String field, String message) {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", field, message));
        return new MethodArgumentNotValidException(null, bindingResult);
    }
}
