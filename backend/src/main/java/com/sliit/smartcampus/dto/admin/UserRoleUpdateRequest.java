package com.sliit.smartcampus.dto.admin;

import com.sliit.smartcampus.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(@NotNull UserRole role) {
}
