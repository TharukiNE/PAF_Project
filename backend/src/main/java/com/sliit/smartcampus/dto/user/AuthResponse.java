package com.sliit.smartcampus.dto.user;

/**
 * Returned by login, register, and auto-login when JWT access tokens are issued.
 */
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {

    public static AuthResponse of(String token, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
