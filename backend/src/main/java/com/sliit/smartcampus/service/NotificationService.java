package com.sliit.smartcampus.service;

import com.sliit.smartcampus.dto.admin.AdminNotificationBroadcastRequest;
import com.sliit.smartcampus.entity.Booking;
import com.sliit.smartcampus.entity.MaintenanceTicket;
import com.sliit.smartcampus.entity.Notification;
import com.sliit.smartcampus.entity.User;
import com.sliit.smartcampus.entity.enums.BookingStatus;
import com.sliit.smartcampus.entity.enums.NotificationType;
import com.sliit.smartcampus.entity.enums.UserRole;
import com.sliit.smartcampus.exception.ApiException;
import com.sliit.smartcampus.repository.CampusResourceRepository;
import com.sliit.smartcampus.repository.NotificationRepository;
import com.sliit.smartcampus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Notification-related business logic.
 * Creates user-facing notifications for booking, ticket, and announcement events.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CampusResourceRepository campusResourceRepository;
    private final UserRepository userRepository;

    /**
     * Notify a booking owner when the booking status changes.
     */
    public void notifyBookingStatusChange(Booking booking, BookingStatus newStatus) {
        String resourceName = campusResourceRepository.findById(booking.getResourceId())
                .map(r -> r.getName())
                .orElse("Resource");
        String reason = booking.getDecisionReason();
        String msg = "Booking #" + booking.getId() + " for " + resourceName
                + " is now " + newStatus.name().replace('_', ' ') + ".";
        if (reason != null && !reason.isBlank()) {
            msg = msg + " Reason: " + reason + ".";
        }
        save(booking.getUserId(), NotificationType.BOOKING_STATUS, msg, "BOOKING", booking.getId());
    }

    /**
     * Notify the ticket reporter and assigned technician when resolution details change.
     */
    public void notifyTicketResolutionUpdated(MaintenanceTicket ticket) {
        String msg = "Ticket #" + ticket.getId() + " \"" + ticket.getTitle() + "\" resolution was updated.";
        save(ticket.getReporterId(), NotificationType.TICKET_UPDATE, msg, "TICKET", ticket.getId());
        if (ticket.getAssignedTechnicianId() != null
                && !ticket.getAssignedTechnicianId().equals(ticket.getReporterId())) {
            save(ticket.getAssignedTechnicianId(), NotificationType.TICKET_UPDATE, msg, "TICKET", ticket.getId());
        }
    }

    /**
     * Notify participants when a new comment is added to a ticket.
     */
    public void notifyTicketCommentAdded(MaintenanceTicket ticket, User author, String preview) {
        Set<String> recipients = new HashSet<>();
        recipients.add(ticket.getReporterId());
        if (ticket.getAssignedTechnicianId() != null) {
            recipients.add(ticket.getAssignedTechnicianId());
        }
        recipients.remove(author.getId());
        String shortPreview = preview.length() > 80 ? preview.substring(0, 77) + "..." : preview;
        String msg = "New comment on ticket #" + ticket.getId() + ": " + shortPreview;
        for (String uid : recipients) {
            save(uid, NotificationType.TICKET_UPDATE, msg, "TICKET", ticket.getId());
        }
    }

    /**
     * Send a campus-wide announcement or a selected student broadcast from an admin.
     */
    public void broadcastAnnouncement(AdminNotificationBroadcastRequest req, User admin) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Administrator only");
        }
        List<String> targetIds;
        if (req.audience() == AdminNotificationBroadcastRequest.Audience.ALL_STUDENTS) {
            targetIds = userRepository.findByRole(UserRole.USER).stream().map(User::getId).toList();
        } else {
            if (req.userIds() == null || req.userIds().isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "userIds is required when audience is SELECTED");
            }
            targetIds = req.userIds().stream().distinct().toList();
        }
        if (targetIds.isEmpty()) {
            return;
        }
        String text = req.message().trim();
        for (String uid : targetIds) {
            userRepository.findById(uid)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown user id: " + uid));
            save(uid, NotificationType.ANNOUNCEMENT, text, "ANNOUNCEMENT", null);
        }
    }

    private void save(String userId, NotificationType type, String message, String relatedType, String relatedId) {
        Notification n = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .relatedEntityType(relatedType)
                .relatedEntityId(relatedId)
                .readFlag(false)
                .build();
        n.touchCreated();
        notificationRepository.save(n);
    }

    /**
     * Fetch all notifications for the specified user.
     */
    public List<Notification> listForUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Return the number of unread notifications for badge display.
     */
    public long unreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFlagFalse(userId);
    }

    /**
     * Mark a single notification as read.
     */
    public void markRead(String notificationId, String userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setReadFlag(true);
        notificationRepository.save(n);
    }

    /**
     * Mark all notifications as read for a specific user.
     */
    public void markAllRead(String userId) {
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        list.forEach(n -> n.setReadFlag(true));
        notificationRepository.saveAll(list);
    }

    /**
     * Delete a single notification after validating ownership.
     */
    public void delete(String notificationId, String userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Clear all notifications for the requested user.
     */
    public void clearAll(String userId) {
        notificationRepository.deleteByUserId(userId);
    }
}
