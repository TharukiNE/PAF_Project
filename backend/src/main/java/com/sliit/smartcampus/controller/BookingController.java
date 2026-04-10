package com.sliit.smartcampus.controller;

import com.sliit.smartcampus.dto.booking.BookingRequest;
import com.sliit.smartcampus.dto.booking.BookingResponse;
import com.sliit.smartcampus.dto.booking.BookingStatusUpdateRequest;
import com.sliit.smartcampus.dto.booking.BookingTimeUpdateRequest;
import com.sliit.smartcampus.security.CurrentUserService;
import com.sliit.smartcampus.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Bookings", description = "Resource reservations: create, list, cancel, delete")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "List bookings", description = "Admins may pass all=true to see every booking.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<BookingResponse> list(@RequestParam(defaultValue = "false") boolean all) {
        return bookingService.listForCurrentUser(currentUserService.requireCurrentUser(), all);
    }

    @Operation(summary = "Get booking by id")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public BookingResponse get(@PathVariable String id) {
        return bookingService.getById(id, currentUserService.requireCurrentUser());
    }

    @Operation(summary = "Create booking request", description = "Creates a PENDING booking for the current user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created"),
            @ApiResponse(responseCode = "400", description = "Invalid input or inactive resource"),
            @ApiResponse(responseCode = "409", description = "Time slot overlaps another booking")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return bookingService.create(request, currentUserService.requireCurrentUser());
    }

    @Operation(summary = "Update booking status (admin)", description = "Approve, reject, or cancel with optional reason.")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse updateStatus(@PathVariable String id, @Valid @RequestBody BookingStatusUpdateRequest request) {
        return bookingService.updateStatus(id, request, currentUserService.requireCurrentUser());
    }

    @Operation(summary = "Reschedule booking", description = "Owner or admin may change start/end for PENDING or APPROVED bookings.")
    @PutMapping("/{id}/times")
    @PreAuthorize("isAuthenticated()")
    public BookingResponse updateTimes(@PathVariable String id, @Valid @RequestBody BookingTimeUpdateRequest request) {
        return bookingService.updateTimes(id, request, currentUserService.requireCurrentUser());
    }

    @Operation(
            summary = "Cancel booking",
            description = "Sets status to CANCELLED. User may cancel own booking; admin may cancel any. Sends a booking notification."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled"),
            @ApiResponse(responseCode = "400", description = "Already closed"),
            @ApiResponse(responseCode = "403", description = "Not allowed")
    })
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public BookingResponse cancel(@PathVariable String id) {
        return bookingService.cancel(id, currentUserService.requireCurrentUser());
    }

    @Operation(
            summary = "Delete or withdraw booking",
            description = """
                    **DELETE semantics:**
                    - **PENDING** — withdraw the request (removed without using cancel flow).
                    - **CANCELLED** or **REJECTED** — remove the row from lists.
                    - **APPROVED** — not allowed; call **POST /{id}/cancel** first, then DELETE if you still want the row removed.
                    Owner or admin (same ownership rules as cancel for non-terminal states)."""
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booking removed"),
            @ApiResponse(responseCode = "400", description = "Invalid state (e.g. still APPROVED)"),
            @ApiResponse(responseCode = "403", description = "Not your booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void delete(@PathVariable String id) {
        bookingService.deleteBooking(id, currentUserService.requireCurrentUser());
    }
}
