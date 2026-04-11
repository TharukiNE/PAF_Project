package com.sliit.smartcampus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single HATEOAS link for REST discoverability.
 * Used within REST responses to provide discoverable action links.
 */
@Schema(description = "A HATEOAS link for REST discoverability")
public record HateoasLink(
        @Schema(description = "The URL target of this link", example = "/api/bookings/123")
        @JsonProperty("href")
        String href,

        @Schema(description = "HTTP method for this link", example = "GET")
        @JsonProperty("method")
        String method,

        @Schema(description = "Human-readable title", example = "Get Booking Details")
        @JsonProperty("title")
        String title
) {
    /**
     * Creates a link with default title from the method and href.
     */
    public HateoasLink(String href, String method) {
        this(href, method, null);
    }
}
