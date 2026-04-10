package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.notification.NotificationResponse;
import com.sliit.smartcampus.dto.notification.UnreadCountResponse;
import com.sliit.smartcampus.security.CurrentUserService;
import com.sliit.smartcampus.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notifications", description = "In-app notifications for the current user")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "List my notifications", description = "Newest first, including announcements from admin broadcast.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<NotificationResponse> list() {
        var user = currentUserService.requireCurrentUser();
        return notificationService.listForUser(user.getId()).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Operation(summary = "Unread notification count", description = "Returns how many notifications are still unread for the current user.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = UnreadCountResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public UnreadCountResponse unreadCount() {
        var user = currentUserService.requireCurrentUser();
        return new UnreadCountResponse(notificationService.unreadCount(user.getId()));
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void markRead(@PathVariable String id) {
        var user = currentUserService.requireCurrentUser();
        notificationService.markRead(id, user.getId());
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void markAllRead() {
        var user = currentUserService.requireCurrentUser();
        notificationService.markAllRead(user.getId());
    }

    @Operation(summary = "Delete one notification", description = "Removes a single notification owned by the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Not your notification"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void delete(@PathVariable String id) {
        var user = currentUserService.requireCurrentUser();
        notificationService.delete(id, user.getId());
    }

    @Operation(summary = "Delete all my notifications", description = "Clears every notification for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All cleared")
    })
    @DeleteMapping("/clear-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void clearAll() {
        var user = currentUserService.requireCurrentUser();
        notificationService.clearAll(user.getId());
    }
}
