package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.notification.NotificationResponse;
import com.sliit.smartcampus.dto.notification.UnreadCountResponse;
import com.sliit.smartcampus.hateoas.NotificationModelAssembler;
import com.sliit.smartcampus.security.CurrentUserService;
import com.sliit.smartcampus.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller for in-app notifications.
 * Exposes endpoints to read, mark read, and delete notifications for the authenticated user.
 */
@Tag(name = "Notifications", description = "In-app notifications (HAL + links) for the current user")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final NotificationModelAssembler notificationAssembler;

    @Operation(
            summary = "List my notifications",
            description = "Retrieves all notifications for the authenticated user, ordered by most recent first. Returns HAL collection " +
                    "with individual notification items containing HATEOAS links for mark-read and delete actions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HAL collection of notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CollectionModel<EntityModel<NotificationResponse>>> list() {
        // Return the current user's notifications ordered by most recent.
        var user = currentUserService.requireCurrentUser();
        List<EntityModel<NotificationResponse>> content = notificationService.listForUser(user.getId()).stream()
                .map(NotificationResponse::from)
                .map(notificationAssembler::toModel)
                .toList();
        CollectionModel<EntityModel<NotificationResponse>> body = CollectionModel.of(content,
                linkTo(methodOn(NotificationController.class).list()).withSelfRel());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(15, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(body);
    }

    @Operation(
            summary = "Get unread notification count",
            description = "Fast, cacheable endpoint returning the count of unread notifications for the current user. " +
                    "Useful for displaying badge count in UI. Includes HATEOAS links to full notifications list."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<UnreadCountResponse>> unreadCount() {
        var user = currentUserService.requireCurrentUser();
        long n = notificationService.unreadCount(user.getId());
        EntityModel<UnreadCountResponse> body = EntityModel.of(new UnreadCountResponse(n),
                linkTo(methodOn(NotificationController.class).unreadCount()).withSelfRel(),
                linkTo(methodOn(NotificationController.class).list()).withRel("notifications"));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(body);
    }

    @Operation(
            summary = "Mark notification as read",
            description = "Marks a single notification as read for the current user. Removes from unread badge count."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notification marked as read"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Not your notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(@PathVariable String id) {
        // Mark a single notification as read for the current user.
        var user = currentUserService.requireCurrentUser();
        notificationService.markRead(id, user.getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Bulk marks every notification for the current user as read. Clears unread badge count."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All notifications marked as read"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllRead() {
        // Mark all notifications as read for the current user.
        var user = currentUserService.requireCurrentUser();
        notificationService.markAllRead(user.getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @Operation(
            summary = "Delete one notification",  
            description = "Permanently removes a single notification owned by the current user. Notification cannot be recovered."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notification deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Not your notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        // Delete a single notification owned by the current user.
        var user = currentUserService.requireCurrentUser();
        notificationService.delete(id, user.getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @Operation(
            summary = "Delete all my notifications",
            description = "Bulk deletes every notification for the current user. All notifications are permanently removed and cannot be recovered."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All notifications cleared successfully")
    })
    @DeleteMapping("/clear-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearAll() {
        // Remove all notifications for the current user.
        var user = currentUserService.requireCurrentUser();
        notificationService.clearAll(user.getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
