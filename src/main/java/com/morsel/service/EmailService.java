package com.morsel.service;

import com.morsel.config.AppProperties;
import com.morsel.config.PasswordResetProperties;
import com.morsel.event.PasswordResetEvent;
import com.morsel.logging.PiiSanitizer;
import io.micrometer.observation.annotation.Observed;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final PasswordResetProperties passwordResetProperties;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetEvent(PasswordResetEvent event) {
        sendPasswordResetEmail(event.email(), event.token());
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@morsel.app");
            helper.setTo(to);
            helper.setSubject("Morsel - Password Reset Request");
            String resetUrl = appProperties.baseUrl() + "/reset-password?token=" + token;
            String htmlContent = """
                    <html>
                    <body>
                        <h2>Password Reset Request</h2>
                        <p>You have requested to reset your password.</p>
                        <p>Click the link below to reset your password:</p>
                        <p><a href="%s">Reset Password</a></p>
                        <p>This link will expire in %d minutes.</p>
                        <p>If you did not request this, please ignore this email.</p>
                    </body>
                    </html>
                    """.formatted(resetUrl, passwordResetProperties.tokenExpiryMinutes());
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.debug("Password reset email sent to {}", PiiSanitizer.sanitizeEmail(to));
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", PiiSanitizer.sanitizeEmail(to), e);
        }
    }
}
