package com.rbms.renbo.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around JavaMailSender.
 * Keeping email construction here means PasswordResetService stays focused
 * on business logic and is easy to test without touching SMTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Sends the password-reset email containing the one-click reset link.
     *
     * @param toEmail   recipient's email address
     * @param token     the opaque reset token
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("RenBoNow — Password Reset Request");
        message.setText(buildEmailBody(resetLink));

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException e) {
            // Log but do not surface SMTP errors to the caller —
            // we respond with the same generic message regardless (security best practice).
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailBody(String resetLink) {
        return """
                Hi,

                We received a request to reset your RenBoNow password.
                Click the link below to choose a new password. This link expires in 1 hour.

                %s

                If you did not request a password reset, you can safely ignore this email.
                Your password will not change.

                — The RenBoNow Team
                """.formatted(resetLink);
    }
}