package com.sliit.smartcampus.dto.user;

import com.sliit.smartcampus.entity.User;
import com.sliit.smartcampus.entity.enums.UserRole;

/**
 * Public view of a user account that is safe to return from API endpoints.
 * This DTO exposes only the fields needed by the frontend and hides private data.
 */
public record UserResponse(String id, String email, String name, UserRole role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getRole());
    }
}
