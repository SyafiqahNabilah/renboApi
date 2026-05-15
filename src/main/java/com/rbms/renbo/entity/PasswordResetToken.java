package com.rbms.renbo.entity;

import jakarta.persistence.*;
        import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores a one-time password-reset token tied to a specific user.
 * A token is valid for a configurable window (default 1 hour) and can only be used once.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /** The opaque, URL-safe reset token sent in the email link. */
    @Column(nullable = false, unique = true)
    private String token;

    /** The user this token belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** When this token stops being valid. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Set to true after the token has been successfully consumed. */
    @Column(nullable = false)
    private boolean used = false;

    /** Convenience constructor used by PasswordResetService. */
    public PasswordResetToken(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}