package com.sliit.smartcampus.service;

import com.sliit.smartcampus.dto.maintenance.*;
import com.sliit.smartcampus.entity.MaintenanceTicket;
import com.sliit.smartcampus.entity.User;
import com.sliit.smartcampus.entity.enums.TicketStatus;
import com.sliit.smartcampus.entity.enums.UserRole;
import com.sliit.smartcampus.exception.ApiException;
import com.sliit.smartcampus.repository.MaintenanceTicketRepository;
import com.sliit.smartcampus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for maintenance ticket lifecycle management.
 * Handles ticket creation, image upload, comments, technician assignment, resolution, and reopen operations.
 */
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    public static final int MAX_IMAGES_PER_TICKET = 3;

    private final MaintenanceTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public List<TicketResponse> listAll() {
        return ticketRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public TicketResponse getById(String id) {
        return toResponse(getTicket(id));
    }

    /**
     * Open a new maintenance ticket from the reporter's request.
     */
    public TicketResponse create(TicketRequest req, User reporter) {
        MaintenanceTicket t = MaintenanceTicket.builder()
                .title(req.title() != null ? req.title() : "")
                .description(req.description())
                .priority(req.priority() != null ? req.priority() : com.sliit.smartcampus.entity.enums.TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .reporterId(reporter.getId())
                .images(new ArrayList<>())
                .comments(new ArrayList<>())
                .build();
        t.touchTimestamps();
        return toResponse(ticketRepository.save(t));
    }

    /**
     * Store an image attachment for a ticket and preserve metadata.
     */
    public TicketImageResponse addImage(String ticketId, MultipartFile file) {
        MaintenanceTicket ticket = getTicket(ticketId);
        if (ticket.getImages().size() >= MAX_IMAGES_PER_TICKET) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum " + MAX_IMAGES_PER_TICKET + " images per ticket");
        }
        String path = fileStorageService.storeTicketImage(file, ticketId);
        MaintenanceTicket.EmbeddedTicketImage img = MaintenanceTicket.EmbeddedTicketImage.builder()
                .id(UUID.randomUUID().toString())
                .storedPath(path)
                .originalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image")
                .uploadedAt(Instant.now())
                .build();
        ticket.getImages().add(img);
        ticket.touchTimestamps();
        ticketRepository.save(ticket);
        return TicketImageResponse.from(img);
    }

    /**
     * Load a protected ticket image file only if the current user may access the ticket.
     */
    public Resource loadImageFile(String imageId, User current) {
        MaintenanceTicket ticket = ticketRepository.findByAnyImageId(imageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image not found"));
        assertTicketAccess(ticket, current);
        MaintenanceTicket.EmbeddedTicketImage img = ticket.getImages().stream()
                .filter(i -> imageId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image not found"));
        Path filePath = fileStorageService.resolveStoredPath(img.getStoredPath());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "File missing");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid file path");
        }
    }

    public MaintenanceTicket.EmbeddedTicketImage getImageMeta(String imageId) {
        MaintenanceTicket ticket = ticketRepository.findByAnyImageId(imageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image not found"));
        return ticket.getImages().stream()
                .filter(i -> imageId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image not found"));
    }

    /**
     * Add a comment to a ticket and notify interested parties.
     */
    public TicketCommentResponse addComment(String ticketId, TicketCommentRequest req, User author) {
        MaintenanceTicket ticket = getTicket(ticketId);
        String content = req.content() != null ? req.content() : "";
        MaintenanceTicket.EmbeddedTicketComment c = MaintenanceTicket.EmbeddedTicketComment.builder()
                .id(UUID.randomUUID().toString())
                .userId(author.getId())
                .userEmail(author.getEmail())
                .content(content)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        ticket.getComments().add(c);
        ticket.touchTimestamps();
        ticketRepository.save(ticket);
        notificationService.notifyTicketCommentAdded(ticket, author, content);
        return TicketCommentResponse.from(c, ticketId);
    }

    /**
     * Edit a comment authored by the current user.
     */
    public TicketCommentResponse updateComment(String commentId, TicketCommentRequest req, User current) {
        MaintenanceTicket ticket = findTicketContainingComment(commentId);
        MaintenanceTicket.EmbeddedTicketComment c = ticket.getComments().stream()
                .filter(x -> commentId.equals(x.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!c.getUserId().equals(current.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only edit your own comments");
        }
        c.setContent(req.content() != null ? req.content() : "");
        c.setUpdatedAt(Instant.now());
        ticket.touchTimestamps();
        ticketRepository.save(ticket);
        return TicketCommentResponse.from(c, ticket.getId());
    }

    /**
     * Delete a comment from a ticket when owned by the current user.
     */
    public void deleteComment(String commentId, User current) {
        MaintenanceTicket ticket = findTicketContainingComment(commentId);
        MaintenanceTicket.EmbeddedTicketComment c = ticket.getComments().stream()
                .filter(x -> commentId.equals(x.getId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!c.getUserId().equals(current.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }
        ticket.getComments().remove(c);
        ticket.touchTimestamps();
        ticketRepository.save(ticket);
    }

    /**
     * Update technician resolution notes and ticket status for qualified users.
     */
    public TicketResponse updateResolution(String ticketId, TicketResolutionRequest req, User current) {
        if (current.getRole() != UserRole.TECHNICIAN && current.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only technicians or administrators can update resolution");
        }
        MaintenanceTicket t = getTicket(ticketId);
        t.setResolutionNotes(req.resolutionNotes() != null ? req.resolutionNotes() : "");
        if (req.status() != null) {
            t.setStatus(req.status());
        }
        t.touchTimestamps();
        MaintenanceTicket saved = ticketRepository.save(t);
        notificationService.notifyTicketResolutionUpdated(saved);
        return toResponse(saved);
    }

    /**
     * Assign a technician to a ticket. Only an administrator may perform this action.
     */
    public TicketResponse assignTechnician(String ticketId, String technicianUserId, User admin) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only administrators can assign technicians");
        }
        if (technicianUserId == null || technicianUserId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        MaintenanceTicket t = getTicket(ticketId);
        User tech = userRepository.findById(technicianUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (tech.getRole() != UserRole.TECHNICIAN && tech.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Target user must be a technician or admin");
        }
        t.setAssignedTechnicianId(tech.getId());
        t.touchTimestamps();
        return toResponse(ticketRepository.save(t));
    }

    /**
     * Reopen a resolved or closed ticket for further work by the owner or support staff.
     */
    public TicketResponse reopen(String ticketId, User current) {
        MaintenanceTicket t = getTicket(ticketId);
        if (t.getStatus() != TicketStatus.RESOLVED && t.getStatus() != TicketStatus.CLOSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only RESOLVED or CLOSED tickets can be reopened");
        }
        assertTicketAccess(t, current);
        t.setStatus(TicketStatus.OPEN);
        t.setResolutionNotes(null);
        t.touchTimestamps();
        return toResponse(ticketRepository.save(t));
    }

    private MaintenanceTicket findTicketContainingComment(String commentId) {
        return ticketRepository.findAll().stream()
                .filter(t -> t.getComments().stream().anyMatch(c -> commentId.equals(c.getId())))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    private MaintenanceTicket getTicket(String id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    /**
     * Ensure current user may access this ticket: reporter, technician, or admin.
     */
    private void assertTicketAccess(MaintenanceTicket ticket, User current) {
        if (current.getRole() == UserRole.ADMIN || current.getRole() == UserRole.TECHNICIAN) {
            return;
        }
        if (ticket.getReporterId().equals(current.getId())) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to access this ticket");
    }

    private TicketResponse toResponse(MaintenanceTicket t) {
        String reporterEmail = userRepository.findById(t.getReporterId())
                .map(User::getEmail)
                .orElse("?");
        List<TicketImageResponse> images = t.getImages().stream()
                .map(TicketImageResponse::from)
                .toList();
        List<TicketCommentResponse> comments = t.getComments().stream()
                .sorted(Comparator.comparing(MaintenanceTicket.EmbeddedTicketComment::getCreatedAt))
                .map(c -> TicketCommentResponse.from(c, t.getId()))
                .toList();
        return TicketResponse.from(t, reporterEmail, images, comments);
    }
}
