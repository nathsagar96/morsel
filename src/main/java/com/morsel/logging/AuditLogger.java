package com.morsel.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuditLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("com.morsel.AUDIT");

    private AuditLogger() {}

    public enum Event {
        REGISTRATION_SUCCESS,
        REGISTRATION_FAILURE_DUPLICATE_USERNAME,
        REGISTRATION_FAILURE_DUPLICATE_EMAIL,
        REGISTRATION_FAILURE_CONSTRAINT_VIOLATION,
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        TOKEN_REFRESH_SUCCESS,
        TOKEN_REFRESH_FAILURE,
        PASSWORD_RESET_INITIATED,
        PASSWORD_RESET_COMPLETED,
        PASSWORD_RESET_FAILURE_INVALID_TOKEN,
        PASSWORD_RESET_FAILURE_EXPIRED_TOKEN,
        PASSWORD_RESET_FAILURE_USED_TOKEN,
        JWT_REJECTED_DISABLED_LOCKED,
        ADMIN_USER_STATUS_CHANGE,
        RECIPE_DELETED,
        INGREDIENT_DELETED
    }

    public enum Outcome {
        SUCCESS,
        FAILURE
    }

    public static void log(Event event, Long userId, Outcome outcome, String detail) {
        String userIdStr = userId != null ? userId.toString() : "anonymous";
        AUDIT_LOG.info(
                "[AUDIT] event={} userId={} outcome={} detail={}",
                event,
                userIdStr,
                outcome,
                detail != null ? detail : "-");
    }

    public static void log(Event event, Long userId, Outcome outcome) {
        log(event, userId, outcome, null);
    }
}
