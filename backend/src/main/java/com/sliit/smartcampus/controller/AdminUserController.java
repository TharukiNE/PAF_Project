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

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> list() {
        return adminUserService.listUsers();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(@PathVariable String id, @Valid @RequestBody UserRoleUpdateRequest request) {
        return adminUserService.updateRole(id, request, currentUserService.requireCurrentUser());
    }
}
