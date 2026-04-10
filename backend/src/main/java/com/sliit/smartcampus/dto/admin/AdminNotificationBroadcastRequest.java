package com.sliit.smartcampus.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Broadcast a campus announcement. Send userIds only when audience is SELECTED.")
public record AdminNotificationBroadcastRequest(
        @Schema(
                description = "Message shown in each recipient’s notification bell",
                example = "New electronics lab is open in Block B — you can book from Monday.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 2000
        )
        @NotBlank @Size(max = 2000) String message,
        @Schema(
                description = "ALL_STUDENTS = every USER role account; SELECTED = only userIds listed",
                example = "ALL_STUDENTS",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull Audience audience,
        @Schema(
                description = "MongoDB user ids to notify; required when audience is SELECTED. Omit or null for ALL_STUDENTS.",
                example = "[\"64b2f8a1c3d4e5f6a7b8c9d0\", \"64b2f8a1c3d4e5f6a7b8c9d1\"]",
                nullable = true
        )
        List<@NotBlank @Size(max = 64) String> userIds
) {
    @Schema(description = "Notification audience")
    public enum Audience {
        /** Every account with role USER (students). */
        ALL_STUDENTS,
        /** Subset of user IDs (must be sent when this audience is chosen). */
        SELECTED
    }
}
