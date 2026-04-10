package com.sliit.smartcampus.dto.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketCommentRequest(@NotBlank @Size(max = 4000) String content) {
}
