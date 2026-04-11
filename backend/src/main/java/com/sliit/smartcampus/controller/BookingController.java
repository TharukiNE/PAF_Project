package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.booking.BookingRequest;
import com.sliit.smartcampus.dto.booking.BookingResponse;
import com.sliit.smartcampus.dto.booking.BookingStatusUpdateRequest;
import com.sliit.smartcampus.dto.booking.BookingTimeUpdateRequest;
import com.sliit.smartcampus.hateoas.BookingModelAssembler;
import com.sliit.smartcampus.security.CurrentUserService;
import com.sliit.smartcampus.service.BookingService;
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
 * Controller for resource booking management.
 * Handles booking requests, listing, cancellation, rescheduling,
 * and admin approval or rejection of reservations.
 */
@Tag(name = "Bookings", description = "Resource reservations (HAL + links): create, list, cancel, delete")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;
    private final BookingModelAssembler bookingAssembler;

    @Operation(
            summary = "List bookings",
            description = "Retrieves all bookings visible to the current user. HAL collection format with _embedded array. " +
                    "Admins may pass all=true to view all bookings across the system; regular users see only their own bookings."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bookings list"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CollectionModel<EntityModel<BookingResponse>>> list(
            @RequestParam(defaultValue = "false") boolean all) {
        // Fetch bookings visible to the current user. Admins can view all bookings.
        List<BookingResponse> raw = bookingService.listForCurrentUser(currentUserService.requireCurrentUser(), all);
        List<EntityModel<BookingResponse>> content = raw.stream()
                .map(b -> bookingAssembler.toModel(b, all))
                .toList();
        CollectionModel<EntityModel<BookingResponse>> body = CollectionModel.of(content,
                linkTo(methodOn(BookingController.class).list(all)).withSelfRel());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(body);
    }

    @Operation(
            summary = "Get booking by ID",
            description = "Retrieves a single booking as a HAL entity with full HATEOAS _links for all discoverable actions. " +
                    "Available actions depend on booking status (PENDING, APPROVED, CANCELLED, REJECTED)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking retrieved successfully with discoverable links"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Not authorized to view this booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<BookingResponse>> get(@PathVariable String id) {
        BookingResponse b = bookingService.getById(id, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(15, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(bookingAssembler.toModel(b, false));
    }

    @Operation(
            summary = "Create booking request",
            description = "Creates a new booking request for the authenticated user. Returns HAL entity with discoverable action links " +
                    "such as cancel, times (reschedule), and status update. Initial status is PENDING and awaits admin approval."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input: malformed request body or resource is inactive"),
            @ApiResponse(responseCode = "409", description = "Time slot conflict: overlaps another booking for the same resource"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<BookingResponse>> create(@Valid @RequestBody BookingRequest request) {
        // Create a new booking request for the authenticated user.
        BookingResponse saved = bookingService.create(request, currentUserService.requireCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(bookingAssembler.toModel(saved, false));
    }

    @Operation(
            summary = "Update booking status (admin only)",
            description = "Admin-only endpoint to approve, reject, or cancel a booking. Returns updated booking with fresh HATEOAS links. " +
                    "Transition statuses: PENDING→APPROVED/REJECTED by admin, or any status→CANCELLED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition or malformed request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "User is not an admin"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<BookingResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody BookingStatusUpdateRequest request) {
        BookingResponse saved = bookingService.updateStatus(id, request, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(bookingAssembler.toModel(saved, true));
    }

    @Operation(
            summary = "Reschedule booking",
            description = "Allows booking owner or admin to change start/end times for PENDING or APPROVED bookings. " +
                    "System validates that new time slot does not overlap other bookings and returns updated booking with HATEOAS links."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking times updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid time range or status prevents rescheduling"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Not owner or admin"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "New time slot conflicts with existing bookings")
    })
    @PutMapping("/{id}/times")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<BookingResponse>> updateTimes(
            @PathVariable String id,
            @Valid @RequestBody BookingTimeUpdateRequest request) {
        // Change booking times while preserving validity and preventing overlaps.
        BookingResponse saved = bookingService.updateTimes(id, request, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(bookingAssembler.toModel(saved, false));
    }

    @Operation(
            summary = "Cancel booking",
            description = "Transitions booking to CANCELLED status. Users may cancel their own bookings; admins may cancel any booking. " +
                    "Cancelled bookings can still be permanently deleted. Returns updated booking with remaining available actions."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Booking already closed (REJECTED or already CANCELLED)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Not allowed: not the booking owner and not an admin"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<BookingResponse>> cancel(@PathVariable String id) {
        BookingResponse saved = bookingService.cancel(id, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(bookingAssembler.toModel(saved, false));
    }

    @Operation(
            summary = "Delete or withdraw booking",
            description = "Permanently deletes a booking record. APPROVED bookings must be CANCELLED first before deletion. " +
                    "Only booking owners and admins may delete. Check HAL _links on the GET booking response to see if deletion is allowed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booking deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid state: APPROVED bookings must be cancelled first"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Not authorized: not the owner or admin"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        bookingService.deleteBooking(id, currentUserService.requireCurrentUser());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
