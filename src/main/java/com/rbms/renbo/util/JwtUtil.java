package com.rbms.renbo.util;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.UserRoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.DecodingException;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private SecretKey signingKey;

    @PostConstruct
    void initializeSigningKey() {
        signingKey = buildSigningKey(jwtSecret);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    private SecretKey buildSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret must not be blank");
        }

        byte[] keyBytes = decodeSecret(secret.trim());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decodeSecret(String secret) {
        // 1. Try Base64 first — accept only if it yields ≥ 32 bytes (256 bits)
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length >= 32) {
                return decoded;
            }
            log.warn("Base64-decoded secret is only {} bits — deriving a 256-bit key via SHA-256", decoded.length * 8);
        } catch (IllegalArgumentException | DecodingException ex) {
            // Not Base64 — fall through to UTF-8 path
        }

        // 2. Hash the raw secret with SHA-256 to guarantee exactly 256 bits
        try {
            return java.security.MessageDigest
                    .getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String generateToken(UUID userId, String email, UserRoleEnum role) {
        log.debug("Generating JWT token for user: {}", email);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("email", email);
        claims.put("role", role.name());

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return (String) claims.get("userId");
        } catch (Exception e) {
            log.error("Error extracting userId from token", e);
            return null;
        }
    }

    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting email from token", e);
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return (String) claims.get("role");
        } catch (Exception e) {
            log.error("Error extracting role from token", e);
            return null;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT validation error", e);
            return false;
        }
    }

    public UUID validateUserExisting(String authHeader){

        // Extract renterId from JWT token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                authHeader.substring(7) : null;

        if (token == null) {
            throw new ApiException(ErrorCodeEnum.UNAUTHORIZED, "Authorization token required");
        }

        String tokenUserStr = extractUserId(token);
        if (tokenUserStr == null) {
            throw new ApiException(ErrorCodeEnum.UNAUTHORIZED, "Invalid token");
        }

        return UUID.fromString(tokenUserStr);
    }
}
