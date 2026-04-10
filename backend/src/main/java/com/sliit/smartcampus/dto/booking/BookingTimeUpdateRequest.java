package com.sliit.smartcampus.dto.booking;

import java.time.Instant;

public record BookingTimeUpdateRequest(Instant startTime, Instant endTime) {
}
