package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.admin.AdminNotificationBroadcastRequest;
import com.sliit.smartcampus.dto.user.UserResponse;
import com.sliit.smartcampus.entity.enums.UserRole;
import com.sliit.smartcampus.repository.UserRepository;
import com.sliit.smartcampus.security.CurrentUserService;
import com.sliit.smartcampus.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin — Notifications", description = "Broadcast announcements to students (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    /**
     * Students (USER role) for targeting broadcast notifications in the admin UI.
     */
    @Operation(summary = "List student recipients", description = "Returns all users with role USER for pick lists when sending SELECTED announcements.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of students"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not an administrator")
    })
    @GetMapping("/recipients")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> listStudentRecipients() {
        return userRepository.findByRole(UserRole.USER).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Operation(
            summary = "Broadcast announcement",
            description = "Creates an ANNOUNCEMENT notification for each target user. Use audience ALL_STUDENTS or SELECTED with userIds."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notifications created"),
            @ApiResponse(responseCode = "400", description = "Validation error or unknown user id"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not an administrator")
    })
    @PostMapping("/broadcast")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void broadcast(@Valid @RequestBody AdminNotificationBroadcastRequest request) {
        notificationService.broadcastAnnouncement(request, currentUserService.requireCurrentUser());
    }
}
