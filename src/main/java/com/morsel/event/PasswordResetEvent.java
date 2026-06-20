package com.morsel.event;

public record PasswordResetEvent(String email, String token) {}
