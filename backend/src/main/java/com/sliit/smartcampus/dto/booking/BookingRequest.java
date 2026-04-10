package com.sliit.smartcampus.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record BookingRequest(
        @NotBlank @Size(max = 64) String resourceId,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @Size(max = 500) String purpose
) {
}
