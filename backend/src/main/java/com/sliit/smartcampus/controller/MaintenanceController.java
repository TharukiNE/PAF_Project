package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.maintenance.*;
import com.sliit.smartcampus.hateoas.MaintenanceTicketModelAssembler;
import com.sliit.smartcampus.hateoas.TicketCommentModelAssembler;
import com.sliit.smartcampus.hateoas.TicketImageModelAssembler;
import com.sliit.smartcampus.security.CurrentUserService;
import com.sliit.smartcampus.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller for maintenance ticket management.
 * Users file tickets, attach images, comment, and technicians/admins resolve issues.
 */
@Tag(name = "Maintenance", description = "Support tickets (HAL + links): list, create, images, comments, resolution")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/maintenance/tickets")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final CurrentUserService currentUserService;
    private final MaintenanceTicketModelAssembler ticketAssembler;
    private final TicketCommentModelAssembler commentAssembler;
    private final TicketImageModelAssembler imageAssembler;

    @Operation(
            summary = "List all maintenance tickets",
            description = "Retrieves all maintenance tickets visible to the current user. HAL collection format with _embedded array. " +
                    "Tickets are sorted by most recent activity first. Tamperproof metadata tracking all participant comments."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tickets list with HATEOAS links"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CollectionModel<EntityModel<TicketResponse>>> list() {
        List<TicketResponse> raw = maintenanceService.listAll();
        List<EntityModel<TicketResponse>> content = raw.stream().map(ticketAssembler::toModel).toList();
        CollectionModel<EntityModel<TicketResponse>> body = CollectionModel.of(content,
                linkTo(methodOn(MaintenanceController.class).list()).withSelfRel());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(body);
    }

    @Operation(
            summary = "Get ticket by ID",
            description = "Retrieves a single maintenance ticket with all associated images and comments. Returns HAL entity with full HATEOAS _links " +
                    "for discoverable actions like assign technician, mark resolution, reopen, or add images/comments."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket retrieved successfully with full details and links"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions to view this ticket"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<TicketResponse>> get(@PathVariable String id) {
        TicketResponse t = maintenanceService.getById(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(15, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(ticketAssembler.toModel(t));
    }

    @Operation(
            summary = "Create new maintenance ticket",
            description = "Opens a new maintenance request for the authenticated user. Assigns to reporter initially. " +
                    "Returns ticket in OPEN status with discoverable action links for adding images, comments, and admin assignment."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket created successfully with discoverable links"),
            @ApiResponse(responseCode = "400", description = "Invalid input: missing required fields or malformed request body"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<TicketResponse>> create(@Valid @RequestBody TicketRequest request) {
        // Create a new maintenance ticket for the authenticated reporter.
        TicketResponse saved = maintenanceService.create(request, currentUserService.requireCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(ticketAssembler.toModel(saved));
    }

    @Operation(
            summary = "Upload image to ticket",
            description = "Attaches an image file to a ticket for diagnostics or proof. Image remains associated and downloadable. " +
                    "Returns uploaded image metadata with link to download."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file format or missing file"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<TicketImageResponse>> uploadImage(
            @PathVariable String id, @RequestPart("file") MultipartFile file) {
        // Attach an image to an existing ticket for diagnostics or proof of issue.
        TicketImageResponse img = maintenanceService.addImage(id, file);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(imageAssembler.toModel(img, id));
    }

    @Operation(
            summary = "Download image file",
            description = "Downloads the original image file for a ticket. Sent inline for browser preview or as attachment download."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image file downloaded successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Image not found")
    })
    @GetMapping("/images/{imageId}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadImage(@PathVariable String imageId) {
        Resource resource = maintenanceService.loadImageFile(imageId, currentUserService.requireCurrentUser());
        var img = maintenanceService.getImageMeta(imageId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + img.getOriginalFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @Operation(
            summary = "Add comment to ticket",
            description = "Posts a new comment or update to an existing ticket. All participants and admins can comment. " +
                    "Comments are tamperproof with creator and timestamp metadata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input: empty comment or malformed request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<TicketCommentResponse>> addComment(
            @PathVariable String id, @Valid @RequestBody TicketCommentRequest request) {
        TicketCommentResponse saved = maintenanceService.addComment(id, request, currentUserService.requireCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(commentAssembler.toModel(saved));
    }

    @Operation(
            summary = "Update comment",
            description = "Edits an existing comment. Only the comment author can edit their own comments. " +
                    "Edit history is preserved for audit purposes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comment updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or empty comment text"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Not authorized: not the comment author"),
            @ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @PutMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<TicketCommentResponse>> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody TicketCommentRequest request) {
        TicketCommentResponse saved = maintenanceService.updateComment(commentId, request, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(commentAssembler.toModel(saved));
    }

    @Operation(
            summary = "Delete comment",
            description = "Removes a comment from a ticket. Only the comment author or admin can delete. Deletion is permanent but logged."  
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Not authorized: not the author or admin"),
            @ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(@PathVariable String commentId) {
        maintenanceService.deleteComment(commentId, currentUserService.requireCurrentUser());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @Operation(
            summary = "Mark ticket as resolved",
            description = "Technician or admin marks a ticket RESOLVED with resolution notes. Transitions ticket to RESOLVED status and " +
                    "generates notification for the reporter. Can be reopened if needed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket marked as resolved"),
            @ApiResponse(responseCode = "400", description = "Invalid state or missing resolution notes"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Only technician or admin can resolve"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PutMapping("/{id}/resolution")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public ResponseEntity<EntityModel<TicketResponse>> resolution(
            @PathVariable String id, @Valid @RequestBody TicketResolutionRequest request) {
        TicketResponse saved = maintenanceService.updateResolution(id, request, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ticketAssembler.toModel(saved));
    }

    @Operation(
            summary = "Assign technician to ticket",
            description = "Admin assigns a technician to a maintenance ticket. Technician receives notification and gains assignment visibility. " +
                    "Can be reassigned to a different technician or unassigned by passing null userId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Technician assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid technician ID or user not found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Only admin can assign technicians"),
            @ApiResponse(responseCode = "404", description = "Ticket or technician not found")
    })
    @PutMapping("/{id}/technician")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<TicketResponse>> assignTechnician(
            @PathVariable String id, @Valid @RequestBody AssignTechnicianRequest body) {
        String techId = body != null ? body.userId() : null;
        TicketResponse saved = maintenanceService.assignTechnician(id, techId, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ticketAssembler.toModel(saved));
    }

    @Operation(
            summary = "Reopen resolved ticket",
            description = "Transitions a RESOLVED or CLOSED ticket back to OPEN status if the issue resurfaces. " +
                    "Original comments and images are preserved. Notifies assigned technician."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket reopened successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot reopen: ticket is not RESOLVED or CLOSED"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Only reporter or admin can reopen"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PostMapping("/{id}/reopen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<TicketResponse>> reopen(@PathVariable String id) {
        TicketResponse saved = maintenanceService.reopen(id, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ticketAssembler.toModel(saved));
    }
}
