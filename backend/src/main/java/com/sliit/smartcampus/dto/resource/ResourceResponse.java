package com.sliit.smartcampus.dto.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sliit.smartcampus.dto.HateoasLink;
import com.sliit.smartcampus.entity.CampusResource;
import com.sliit.smartcampus.entity.enums.ResourceStatus;
import com.sliit.smartcampus.entity.enums.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Schema(description = "Campus resource response with HATEOAS links for discoverable actions")
public record ResourceResponse(
        @Schema(description = "Resource ID", example = "64b2f8a1c3d4e5f6a7b8c9d0")
        String id,

        @Schema(description = "Resource name", example = "Meeting Room A")
        String name,

        @Schema(description = "Resource type", example = "ROOM")
        ResourceType type,

        @Schema(description = "Maximum capacity", example = "10")
        Integer capacity,

        @Schema(description = "Physical location", example = "Building B")
        String location,

        @Schema(description = "Floor number", example = "3")
        String floor,

        @Schema(description = "Available amenities", example = "[\"Projector\", \"Whiteboard\"]")
        List<String> amenities,

        @Schema(description = "Resource availability status", example = "ACTIVE")
        ResourceStatus status,

        @Schema(description = "Creation timestamp (ISO-8601)")
        Instant createdAt,

        @Schema(description = "Last updated timestamp (ISO-8601)")
        Instant updatedAt,

        @Schema(description = "HATEOAS links for discoverable actions")
        @JsonProperty("_links")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, HateoasLink> links
) {
    public static ResourceResponse from(CampusResource r) {
        return new ResourceResponse(
                r.getId(), r.getName(), r.getType(), r.getCapacity(), r.getLocation(),
                r.getFloor(), r.getAmenities() != null ? r.getAmenities() : List.of(),
                r.getStatus(), r.getCreatedAt(), r.getUpdatedAt(),
                new HashMap<>()
        );
    }
}
