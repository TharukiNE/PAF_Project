package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.admin.UserRoleUpdateRequest;
import com.sliit.smartcampus.dto.user.UserResponse;
import com.sliit.smartcampus.security.CurrentUserService;
import com.sliit.smartcampus.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Exposes admin-only endpoints for listing users and changing their roles.
 * This controller is responsible for member management by administrators.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;

    /**
     * Returns a list of all registered users.
     * Only administrators may call this endpoint.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> list() {
        return adminUserService.listUsers();
    }

    /**
     * Updates a single user's role based on the provided request.
     * The authenticated admin user is validated before the change is applied.
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(@PathVariable String id, @Valid @RequestBody UserRoleUpdateRequest request) {
        return adminUserService.updateRole(id, request, currentUserService.requireCurrentUser());
    }
}
