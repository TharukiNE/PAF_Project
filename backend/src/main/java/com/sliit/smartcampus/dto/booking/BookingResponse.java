package com.sliit.smartcampus.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliit.smartcampus.dto.HateoasLink;
import com.sliit.smartcampus.entity.Booking;
import com.sliit.smartcampus.entity.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Schema(description = "Booking reservation response with HATEOAS links for discoverable actions")
public record BookingResponse(
        @Schema(description = "Booking ID", example = "64b2f8a1c3d4e5f6a7b8c9d0")
        String id,

        @Schema(description = "Resource ID", example = "64b2f8a1c3d4e5f6a7b8c9d1")
        String resourceId,

        @Schema(description = "Resource name", example = "Meeting Room A")
        String resourceName,

        @Schema(description = "User ID", example = "64b2f8a1c3d4e5f6a7b8c9d2")
        String userId,

        @Schema(description = "User email", example = "user@example.com")
        String userEmail,

        @Schema(description = "Booking start time (ISO-8601)")
        Instant startTime,

        @Schema(description = "Booking end time (ISO-8601)")
        Instant endTime,

        @Schema(description = "Booking status", example = "PENDING")
        BookingStatus status,

        @Schema(description = "Booking purpose", example = "Team meeting")
        String purpose,

        @Schema(description = "Admin decision reason", example = "Approved", nullable = true)
        String decisionReason,

        @Schema(description = "Creation timestamp (ISO-8601)")
        Instant createdAt,

        @Schema(description = "HATEOAS links for discoverable actions")
        @JsonProperty("_links")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, HateoasLink> links
) {
    public static BookingResponse from(Booking b, String resourceName, String userEmail) {
        return new BookingResponse(
                b.getId(),
                b.getResourceId(),
                resourceName,
                b.getUserId(),
                userEmail,
                b.getStartTime(),
                b.getEndTime(),
                b.getStatus(),
                b.getPurpose(),
                b.getDecisionReason(),
                b.getCreatedAt(),
                new HashMap<>()
        );
    }
}
