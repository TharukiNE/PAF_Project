package com.sliit.smartcampus.dto.maintenance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliit.smartcampus.dto.HateoasLink;
import com.sliit.smartcampus.entity.MaintenanceTicket;
import com.sliit.smartcampus.entity.enums.TicketPriority;
import com.sliit.smartcampus.entity.enums.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Schema(description = "Maintenance ticket response with HATEOAS links for discoverable actions")
public record TicketResponse(
        @Schema(description = "Ticket ID", example = "64b2f8a1c3d4e5f6a7b8c9d0")
        String id,

        @Schema(description = "Problem title", example = "Broken projector in Room A")
        String title,

        @Schema(description = "Detailed description of the issue")
        String description,

        @Schema(description = "Priority level", example = "HIGH")
        TicketPriority priority,

        @Schema(description = "Current ticket status", example = "OPEN")
        TicketStatus status,

        @Schema(description = "Reporter user ID")
        String reporterId,

        @Schema(description = "Reporter email", example = "user@example.com")
        String reporterEmail,

        @Schema(description = "Assigned technician ID (if assigned)", nullable = true)
        String assignedTechnicianId,

        @Schema(description = "Resolution notes (if resolved)", nullable = true)
        String resolutionNotes,

        @Schema(description = "Created timestamp (ISO-8601)")
        Instant createdAt,

        @Schema(description = "Last updated timestamp (ISO-8601)")
        Instant updatedAt,

        @Schema(description = "Associated images for diagnostics")
        List<TicketImageResponse> images,

        @Schema(description = "Comments and updates on the ticket")
        List<TicketCommentResponse> comments,

        @Schema(description = "HATEOAS links for discoverable actions")
        @JsonProperty("_links")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, HateoasLink> links
) {
    public static TicketResponse from(
            MaintenanceTicket t,
            String reporterEmail,
            List<TicketImageResponse> images,
            List<TicketCommentResponse> comments) {
        return new TicketResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getPriority(),
                t.getStatus(),
                t.getReporterId(),
                reporterEmail,
                t.getAssignedTechnicianId(),
                t.getResolutionNotes(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                images,
                comments,
                new HashMap<>()
        );
    }
}
