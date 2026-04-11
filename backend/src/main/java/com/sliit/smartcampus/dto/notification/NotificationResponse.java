package com.sliit.smartcampus.dto.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliit.smartcampus.dto.HateoasLink;
import com.sliit.smartcampus.entity.Notification;
import com.sliit.smartcampus.entity.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * API shape uses JSON key {@code "read"} (not {@code readFlag}) for compatibility with the web client.
 * The Java name {@code readFlag} avoids OpenAPI schema clashes with the word {@code read}.
 */
@Schema(description = "In-app notification for the signed-in user with HATEOAS links")
public record NotificationResponse(
        @Schema(description = "Notification id", example = "64b2f8a1c3d4e5f6a7b8c9d0")
        String id,

        @Schema(description = "Category", example = "ANNOUNCEMENT")
        NotificationType type,

        @Schema(description = "Human-readable message")
        String message,

        @Schema(description = "Related domain type, e.g. BOOKING, TICKET, ANNOUNCEMENT", nullable = true)
        String relatedEntityType,

        @Schema(description = "Related entity id", nullable = true)
        String relatedEntityId,

        @Schema(description = "Whether the user has marked this notification as read", name = "read")
        @JsonProperty("read")
        boolean readFlag,

        @Schema(description = "Created timestamp (ISO-8601)")
        Instant createdAt,

        @Schema(description = "HATEOAS links for discoverable actions")
        @JsonProperty("_links")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, HateoasLink> links
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getRelatedEntityType(),
                n.getRelatedEntityId(),
                n.isReadFlag(),
                n.getCreatedAt(),
                new HashMap<>()
        );
    }
}
