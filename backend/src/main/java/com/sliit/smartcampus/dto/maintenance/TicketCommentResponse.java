package com.sliit.smartcampus.dto.maintenance;

import com.sliit.smartcampus.entity.MaintenanceTicket;

import java.time.Instant;

public record TicketCommentResponse(
        String id,
        String userId,
        String userEmail,
        String content,
        Instant createdAt,
        Instant updatedAt,
        String ticketId
) {
    public static TicketCommentResponse from(MaintenanceTicket.EmbeddedTicketComment c, String ticketId) {
        return new TicketCommentResponse(
                c.getId(),
                c.getUserId(),
                c.getUserEmail(),
                c.getContent(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                ticketId
        );
    }
}
