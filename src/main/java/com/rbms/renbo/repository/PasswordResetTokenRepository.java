package com.rbms.renbo.repository;

import com.rbms.renbo.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Deletes all tokens belonging to a user — called before issuing a new one. */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.userID = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    /** Housekeeping: remove stale expired tokens (run via @Scheduled or manually). */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now")
    void deleteAllExpiredBefore(@Param("now") LocalDateTime now);
}