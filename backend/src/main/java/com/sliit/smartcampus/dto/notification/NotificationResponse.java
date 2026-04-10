package com.sliit.smartcampus.dto.notification;

import com.sliit.smartcampus.entity.Notification;
import com.sliit.smartcampus.entity.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        String id,
        NotificationType type,
        String message,
        String relatedEntityType,
        String relatedEntityId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getRelatedEntityType(),
                n.getRelatedEntityId(),
                n.isReadFlag(),
                n.getCreatedAt()
        );
    }
}
