package com.sliit.smartcampus.security;

import com.sliit.smartcampus.config.AppProperties;
import com.sliit.smartcampus.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final int MIN_SECRET_LENGTH = 32;

    private final AppProperties appProperties;

    public String generateToken(User user) {
        long expSec = Math.max(60, appProperties.getJwt().getExpirationSeconds());
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expSec);
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey())
                .compact();
    }

    public long getExpirationSeconds() {
        return Math.max(60, appProperties.getJwt().getExpirationSeconds());
    }

    public Claims parseAndValidate(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return parseAndValidate(token).getSubject();
    }

    private SecretKey signingKey() {
        String secret = appProperties.getJwt().getSecret();
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_SECRET_LENGTH + " characters; set JWT_SECRET in .env.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
