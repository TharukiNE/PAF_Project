package com.sliit.smartcampus.dto.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignTechnicianRequest(@NotBlank @Size(max = 64) String userId) {
}
