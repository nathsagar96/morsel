package com.morsel.constants;

public final class ErrorMessages {

    private ErrorMessages() {}

    public static final String VALIDATION_FAILURE_TITLE = "Validation Failure";

    public static final String UNAUTHORIZED_TITLE = "Unauthorized";
    public static final String INVALID_CREDENTIALS = "Invalid username or password";

    public static final String FORBIDDEN_TITLE = "Forbidden";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String ACCOUNT_DISABLED = "Account is disabled";

    public static final String TOO_MANY_REQUESTS_TITLE = "Too Many Requests";
    public static final String ACCOUNT_LOCKED = "Account is temporarily locked due to too many failed login attempts";

    public static final String INTERNAL_SERVER_ERROR_TITLE = "Internal Server Error";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred";

    public static final String MALFORMED_REQUEST_TITLE = "Bad Request";
    public static final String MALFORMED_REQUEST = "The request body is malformed or missing";

    public static final String MISSING_PART_TITLE = "Bad Request";
    public static final String MISSING_PART = "A required part of the request is missing";

    public static final String PAYLOAD_TOO_LARGE_TITLE = "Payload Too Large";
    public static final String PAYLOAD_TOO_LARGE = "The uploaded file exceeds the maximum allowed size";

    public static final String INVALID_PARAMETER_TITLE = "Bad Request";
    public static final String INVALID_PARAMETER = "Invalid request parameter";

    public static final String CONSTRAINT_VIOLATION_TITLE = "Validation Failure";
    public static final String CONSTRAINT_VIOLATION = "A constraint was violated";

    public static final String BINDING_FAILURE_TITLE = "Validation Failure";
    public static final String BINDING_FAILURE = "Request parameters failed validation";

    public static final String MESSAGE_KEY = "message";
}
