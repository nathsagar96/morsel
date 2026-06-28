package com.morsel.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.config.AppProperties;
import com.morsel.config.PasswordResetProperties;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AppProperties appProperties;

    @Mock
    private PasswordResetProperties passwordResetProperties;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sends password reset email")
    void sendPasswordResetEmail_sendsMail() {
        when(appProperties.baseUrl()).thenReturn("http://localhost:3000");
        when(passwordResetProperties.tokenExpiryMinutes()).thenReturn(15);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail("test@example.com", "reset-token-123");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }
}
