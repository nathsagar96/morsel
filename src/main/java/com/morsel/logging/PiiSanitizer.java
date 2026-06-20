package com.morsel.logging;

public final class PiiSanitizer {

    private PiiSanitizer() {}

    public static String sanitizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return username;
        }
        if (username.length() <= 2) {
            return "***";
        }
        return username.charAt(0) + "*".repeat(username.length() - 2) + username.charAt(username.length() - 1);
    }

    public static String sanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 0) {
            return sanitizeUsername(email);
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        return sanitizeUsername(localPart) + "@" + sanitizeDomain(domain);
    }

    public static String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return identifier;
        }
        if (identifier.contains("@")) {
            return sanitizeEmail(identifier);
        }
        return sanitizeUsername(identifier);
    }

    private static String sanitizeDomain(String domain) {
        if (domain.isEmpty()) {
            return "***";
        }
        int dotIndex = domain.lastIndexOf('.');
        if (dotIndex < 0) {
            return sanitizeUsername(domain);
        }
        String name = domain.substring(0, dotIndex);
        String tld = domain.substring(dotIndex + 1);
        return sanitizeUsername(name) + "." + sanitizeUsername(tld);
    }
}
