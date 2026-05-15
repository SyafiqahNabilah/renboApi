package com.rbms.renbo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setup() {
        // Inject values manually since @Value won't work in unit test
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@example.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void shouldSendPasswordResetEmailSuccessfully() {
        String toEmail = "user@test.com";
        String token = "abc123";

        emailService.sendPasswordResetEmail(toEmail, token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertEquals("test@example.com", sentMessage.getFrom());
        assertNotNull(sentMessage.getTo());
        assertEquals(toEmail, sentMessage.getTo()[0]);
        assertEquals("RenBoNow — Password Reset Request", sentMessage.getSubject());

        String expectedLink = "http://localhost:3000/reset-password?token=" + token;
        assertNotNull(sentMessage.getText());
        assertTrue(sentMessage.getText().contains(expectedLink));
    }

    @Test
    void shouldHandleMailExceptionGracefully() {
        String toEmail = "user@test.com";
        String token = "abc123";

        doThrow(new MailSendException("SMTP error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Should not throw exception
        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail(toEmail, token)
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}