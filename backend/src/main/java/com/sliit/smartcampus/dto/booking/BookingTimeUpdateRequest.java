package com.sliit.smartcampus.dto.booking;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record BookingTimeUpdateRequest(@NotNull Instant startTime, @NotNull Instant endTime) {
}
