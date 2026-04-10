package com.sliit.smartcampus.dto.resource;

import com.sliit.smartcampus.entity.enums.ResourceStatus;
import com.sliit.smartcampus.entity.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ResourceRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull ResourceType type,
        @Positive Integer capacity,
        @Size(max = 200) String location,
        @Size(max = 80) String floor,
        List<String> amenities,
        ResourceStatus status
) {}

