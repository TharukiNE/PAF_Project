package com.sliit.smartcampus.service;

import com.sliit.smartcampus.dto.booking.BookingRequest;
import com.sliit.smartcampus.dto.booking.BookingResponse;
import com.sliit.smartcampus.dto.booking.BookingStatusUpdateRequest;
import com.sliit.smartcampus.dto.booking.BookingTimeUpdateRequest;
import com.sliit.smartcampus.entity.Booking;
import com.sliit.smartcampus.entity.CampusResource;
import com.sliit.smartcampus.entity.User;
import com.sliit.smartcampus.entity.enums.BookingStatus;
import com.sliit.smartcampus.entity.enums.ResourceStatus;
import com.sliit.smartcampus.entity.enums.UserRole;
import com.sliit.smartcampus.exception.ApiException;
import com.sliit.smartcampus.repository.BookingRepository;
import com.sliit.smartcampus.repository.CampusResourceRepository;
import com.sliit.smartcampus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Set<BookingStatus> BLOCKING_STATUSES = EnumSet.of(BookingStatus.PENDING, BookingStatus.APPROVED);

    private final BookingRepository bookingRepository;
    private final CampusResourceService campusResourceService;
    private final CampusResourceRepository campusResourceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BookingOverlapChecker bookingOverlapChecker;

    public List<BookingResponse> listForCurrentUser(User current, boolean adminView) {
        if (adminView && current.getRole() == UserRole.ADMIN) {
            return bookingRepository.findAllByOrderByStartTimeDesc().stream().map(this::toResponse).toList();
        }
        return bookingRepository.findByUserIdOrderByStartTimeDesc(current.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public BookingResponse getById(String id, User current) {
        Booking b = getBooking(id);
        assertCanView(b, current);
        return toResponse(b);
    }

    public BookingResponse create(BookingRequest req, User current) {
        if (req.resourceId() == null || req.resourceId().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "resourceId is required");
        }
        if (req.startTime() == null || req.endTime() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "start and end time are required");
        }
        validateTimeRange(req.startTime(), req.endTime());
        var resource = campusResourceService.getEntity(req.resourceId());
        if (resource.getStatus() != ResourceStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resource is not available for booking");
        }
        assertNoOverlap(req.resourceId(), req.startTime(), req.endTime(), null);

        Booking b = Booking.builder()
                .resourceId(resource.getId())
                .userId(current.getId())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .status(BookingStatus.PENDING)
                .purpose(req.purpose())
                .build();
        b.touchCreated();
        return toResponse(bookingRepository.save(b));
    }

    public BookingResponse updateStatus(String id, BookingStatusUpdateRequest req, User current) {
        if (current.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only administrators can approve or reject bookings");
        }
        Booking b = getBooking(id);
        if (req.status() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "status is required");
        }
        String reason = req.reason() != null ? req.reason().trim() : null;
        if (reason != null && reason.isBlank()) reason = null;
        BookingStatus newStatus = req.status();
        if (newStatus != BookingStatus.APPROVED
                && newStatus != BookingStatus.REJECTED
                && newStatus != BookingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status transition target");
        }
        if (newStatus == BookingStatus.REJECTED
                && (reason == null || reason.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A decision reason is required when rejecting a booking");
        }
        if (newStatus == BookingStatus.APPROVED) {
            assertNoOverlap(b.getResourceId(), b.getStartTime(), b.getEndTime(), b.getId());
        }
        b.setStatus(newStatus);
        b.setDecisionReason(reason);
        Booking saved = bookingRepository.save(b);
        notificationService.notifyBookingStatusChange(saved, newStatus);
        return toResponse(saved);
    }

    public BookingResponse cancel(String id, User current) {
        Booking b = getBooking(id);
        if (current.getRole() != UserRole.ADMIN && !b.getUserId().equals(current.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only cancel your own bookings");
        }
        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking is already closed");
        }
        b.setStatus(BookingStatus.CANCELLED);
        b.setDecisionReason(null);
        Booking saved = bookingRepository.save(b);
        notificationService.notifyBookingStatusChange(saved, BookingStatus.CANCELLED);
        return toResponse(saved);
    }

    public BookingResponse updateTimes(String id, BookingTimeUpdateRequest req, User current) {
        Booking b = getBooking(id);
        if (!b.getUserId().equals(current.getId()) && current.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to edit this booking");
        }
        if (b.getStatus() != BookingStatus.PENDING && b.getStatus() != BookingStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot reschedule a closed booking");
        }
        if (req.startTime() == null || req.endTime() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "start and end time are required");
        }
        validateTimeRange(req.startTime(), req.endTime());
        if (BLOCKING_STATUSES.contains(b.getStatus())) {
            assertNoOverlap(b.getResourceId(), req.startTime(), req.endTime(), b.getId());
        }
        b.setStartTime(req.startTime());
        b.setEndTime(req.endTime());
        return toResponse(bookingRepository.save(b));
    }

    private void assertNoOverlap(String resourceId, Instant start, Instant end, String excludeBookingId) {
        boolean overlap = bookingOverlapChecker.existsOverlapping(
                resourceId, BLOCKING_STATUSES, start, end, excludeBookingId);
        if (overlap) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This time range overlaps an existing pending or approved booking for the same resource");
        }
    }

    private static void validateTimeRange(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private Booking getBooking(String id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private void assertCanView(Booking b, User current) {
        if (current.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!b.getUserId().equals(current.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed to view this booking");
        }
    }

    private BookingResponse toResponse(Booking b) {
        String resourceName = campusResourceRepository.findById(b.getResourceId())
                .map(CampusResource::getName)
                .orElse("?");
        String userEmail = userRepository.findById(b.getUserId())
                .map(User::getEmail)
                .orElse("?");
        return BookingResponse.from(b, resourceName, userEmail);
    }
}
