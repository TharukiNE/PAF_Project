package com.sliit.smartcampus.dto.user;

import com.sliit.smartcampus.entity.User;
import com.sliit.smartcampus.entity.enums.UserRole;

public record UserResponse(String id, String email, String name, UserRole role) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getName(), u.getRole());
    }
}
