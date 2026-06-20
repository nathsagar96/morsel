package com.morsel.service;

public interface EmailService {

    void sendPasswordResetEmail(String to, String token);
}
