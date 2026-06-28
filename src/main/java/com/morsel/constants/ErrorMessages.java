package com.morsel.constants;

public final class ErrorMessages {

    private ErrorMessages() {}

    public static final String UNAUTHORIZED_TITLE = "Unauthorized";
    public static final String INVALID_CREDENTIALS = "Invalid username or password";

    public static final String FORBIDDEN_TITLE = "Forbidden";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String ACCOUNT_DISABLED = "Account is disabled";

    public static final String TOO_MANY_REQUESTS_TITLE = "Too Many Requests";
    public static final String ACCOUNT_LOCKED = "Account is temporarily locked due to too many failed login attempts";

    public static final String INTERNAL_SERVER_ERROR_TITLE = "Internal Server Error";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred";

    public static final String PAYLOAD_TOO_LARGE_TITLE = "Payload Too Large";
    public static final String PAYLOAD_TOO_LARGE = "The uploaded file exceeds the maximum allowed size";

    public static final String CONSTRAINT_VIOLATION_TITLE = "Validation Failure";

    public static final String BINDING_FAILURE_TITLE = "Validation Failure";

    public static final String WEAK_PASSWORD =
            "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character";

    public static final String MESSAGE_KEY = "message";
}
