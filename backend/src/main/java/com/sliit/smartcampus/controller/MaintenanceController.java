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

    @Operation(summary = "List tickets", description = "HAL collection in _embedded; newest activity first.")
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

    @Operation(summary = "Get ticket by id", description = "Single ticket as HAL entity with _links.")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<TicketResponse>> get(@PathVariable String id) {
        TicketResponse t = maintenanceService.getById(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(15, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(ticketAssembler.toModel(t));
    }

    @Operation(summary = "Create ticket", description = "Returns HAL entity with discoverable action links.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket created"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
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

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<TicketCommentResponse>> addComment(
            @PathVariable String id, @Valid @RequestBody TicketCommentRequest request) {
        TicketCommentResponse saved = maintenanceService.addComment(id, request, currentUserService.requireCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(commentAssembler.toModel(saved));
    }

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

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(@PathVariable String commentId) {
        maintenanceService.deleteComment(commentId, currentUserService.requireCurrentUser());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PutMapping("/{id}/resolution")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN')")
    public ResponseEntity<EntityModel<TicketResponse>> resolution(
            @PathVariable String id, @Valid @RequestBody TicketResolutionRequest request) {
        TicketResponse saved = maintenanceService.updateResolution(id, request, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ticketAssembler.toModel(saved));
    }

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

    @PostMapping("/{id}/reopen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<TicketResponse>> reopen(@PathVariable String id) {
        TicketResponse saved = maintenanceService.reopen(id, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ticketAssembler.toModel(saved));
    }
}
