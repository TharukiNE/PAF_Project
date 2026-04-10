package com.sliit.smartcampus.dto.booking;

import com.sliit.smartcampus.entity.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookingStatusUpdateRequest(
        @NotNull BookingStatus status,
        @Size(max = 2000) String reason
) {
}
