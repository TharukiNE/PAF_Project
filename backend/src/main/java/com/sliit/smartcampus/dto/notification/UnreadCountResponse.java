package com.sliit.smartcampus.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unread notification count for the current user")
public record UnreadCountResponse(
        @Schema(description = "Number of notifications with read=false", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        long count
) {
}
