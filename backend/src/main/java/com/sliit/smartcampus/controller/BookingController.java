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

    @Operation(summary = "List bookings", description = "HAL collection in _embedded. Admins may pass all=true.")
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

    @Operation(summary = "Get booking by id", description = "Single booking as HAL entity with _links.")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EntityModel<BookingResponse>> get(@PathVariable String id) {
        BookingResponse b = bookingService.getById(id, currentUserService.requireCurrentUser());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(15, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(bookingAssembler.toModel(b, false));
    }

    @Operation(summary = "Create booking request", description = "Returns HAL entity with discoverable action links.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created"),
            @ApiResponse(responseCode = "400", description = "Invalid input or inactive resource"),
            @ApiResponse(responseCode = "409", description = "Time slot overlaps another booking")
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

    @Operation(summary = "Update booking status (admin)", description = "Approve, reject, or cancel with optional reason.")
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

    @Operation(summary = "Reschedule booking", description = "Owner or admin may change start/end for PENDING or APPROVED bookings.")
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
            description = "Sets status to CANCELLED. User may cancel own booking; admin may cancel any."
    )
    // Cancel a booking and notify the user of the closed reservation.
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled"),
            @ApiResponse(responseCode = "400", description = "Already closed"),
            @ApiResponse(responseCode = "403", description = "Not allowed")
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
            description = "See HAL delete link on entity when allowed. APPROVED bookings must be cancelled first."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booking removed"),
            @ApiResponse(responseCode = "400", description = "Invalid state"),
            @ApiResponse(responseCode = "403", description = "Not your booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        bookingService.deleteBooking(id, currentUserService.requireCurrentUser());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
