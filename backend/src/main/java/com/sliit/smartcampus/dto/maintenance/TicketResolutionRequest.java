package com.sliit.smartcampus.dto.maintenance;

import com.sliit.smartcampus.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketResolutionRequest(
        @Size(max = 8000) String resolutionNotes,
        @NotNull TicketStatus status
) {
}
