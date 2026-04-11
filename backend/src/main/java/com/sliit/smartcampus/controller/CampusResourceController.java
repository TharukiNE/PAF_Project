package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.resource.ResourceRequest;
import com.sliit.smartcampus.dto.resource.ResourceResponse;
import com.sliit.smartcampus.hateoas.CampusResourceModelAssembler;
import com.sliit.smartcampus.service.CampusResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller for campus facilities and resources.
 * Users may view active resources; admins may create, update, and delete them.
 */
@Tag(name = "Resources", description = "Campus facilities and resources (HAL + links): list, view, create, update, delete")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class CampusResourceController {

    private final CampusResourceService campusResourceService;
    private final CampusResourceModelAssembler resourceAssembler;

    @Operation(
            summary = "List all campus resources",
            description = "Retrieves all available campus resources (meeting rooms, labs, facilities). Returns HAL collection with HATEOAS links. " +
                    "Resources must be in ACTIVE status to appear in results. Cached for 60 seconds."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resources list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CollectionModel<EntityModel<ResourceResponse>>> list() {
        // Return all resources available in the system for the current user.
        List<ResourceResponse> raw = campusResourceService.findAll();
        List<EntityModel<ResourceResponse>> content = raw.stream()
                .map(resourceAssembler::toModel)
                .toList();
        CollectionModel<EntityModel<ResourceResponse>> body = CollectionModel.of(content,
                linkTo(methodOn(CampusResourceController.class).list()).withSelfRel());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(body);
    }

    @Operation(
            summary = "Get resource by ID",
            description = "Retrieves a single campus resource with full details including capacity, amenities, and status. " +
                    "Returns HAL entity with discoverable links for updating and deleting (admin only)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<ResourceResponse>> get(@PathVariable String id) {
        ResourceResponse r = campusResourceService.findById(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(resourceAssembler.toModel(r));
    }

    @Operation(
            summary = "Create new resource (admin only)",
            description = "Admin creates a new campus resource for booking. Defines capacity, type, location, and available amenities. " +
                    "Resource starts in ACTIVE status and is immediately available for booking."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Resource created successfully with discoverable links"),
            @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Only admins can create resources")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<ResourceResponse>> create(@Valid @RequestBody ResourceRequest request) {
        // Admin creates a new campus resource for booking and facility management.
        ResourceResponse saved = campusResourceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(resourceAssembler.toModel(saved));
    }

    @Operation(
            summary = "Update resource (admin only)",
            description = "Admin updates an existing resource's details like capacity, amenities, or status. " +
                    "Changes take effect immediately and may affect existing bookings visibility."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resource updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Only admins can update resources"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<ResourceResponse>> update(
        // Admin updates the fields of an existing resource.
            @PathVariable String id,
            @Valid @RequestBody ResourceRequest request) {
        ResourceResponse saved = campusResourceService.update(id, request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resourceAssembler.toModel(saved));
    }

    @Operation(
            summary = "Delete resource (admin only)",
            description = "Admin permanently deletes a campus resource. Associated bookings will be orphaned. " +
                    "Consider setting resource status to INACTIVE instead of deleting if preservation is needed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Resource deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Only admins can delete resources"),
            @ApiResponse(responseCode = "404", description = "Resource not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        campusResourceService.delete(id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
