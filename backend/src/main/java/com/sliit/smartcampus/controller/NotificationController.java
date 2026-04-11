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

@Tag(name = "Notifications", description = "In-app notifications (HAL + links) for the current user")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final NotificationModelAssembler notificationAssembler;

    @Operation(summary = "List my notifications", description = "HAL collection in _embedded; newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HAL collection"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CollectionModel<EntityModel<NotificationResponse>>> list() {
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

    @Operation(summary = "Unread notification count", description = "HAL entity with _links to the notification collection.")
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

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(@PathVariable String id) {
        var user = currentUserService.requireCurrentUser();
        notificationService.markRead(id, user.getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllRead() {
        var user = currentUserService.requireCurrentUser();
        notificationService.markAllRead(user.getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @Operation(summary = "Delete one notification", description = "Removes a single notification owned by the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Not your notification"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        var user = currentUserService.requireCurrentUser();
        notificationService.delete(id, user.getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @Operation(summary = "Delete all my notifications", description = "Clears every notification for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "All cleared")
    })
    @DeleteMapping("/clear-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearAll() {
        var user = currentUserService.requireCurrentUser();
        notificationService.clearAll(user.getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
