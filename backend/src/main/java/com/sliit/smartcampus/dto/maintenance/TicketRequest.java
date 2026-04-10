package com.sliit.smartcampus.dto.maintenance;

import com.sliit.smartcampus.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 8000) String description,
        @NotNull TicketPriority priority
) {
}
