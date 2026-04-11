package com.sliit.smartcampus.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI smartCampusOpenAPI() {
        final String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Campus Operations Hub API")
                        .description("""
                                REST API for IT3030 PAF — resources, bookings, maintenance, notifications, admin.

                                **Stateless API access:** Authenticated `/api/**` calls use `Authorization: Bearer <JWT>` per request; the server does not rely on session state for API authorization (OAuth browser login may still use a session only for the redirect flow).

                                **HATEOAS (HAL):** `GET /api/bookings` and `GET /api/resources` return HAL-style JSON with `_embedded` items and `_links` (`self`, `bookings`/`resources`, actions where applicable). Clients may follow links instead of hard-coding URLs.

                                **Cacheable responses:** Safe GETs on bookings/resources include `Cache-Control: private, max-age=…` and `Vary: Authorization` so private caches can reuse responses per user token.""")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(bearer))
                .components(new Components()
                        .addSecuritySchemes(bearer, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Use `accessToken` from POST /api/auth/login or /api/auth/register. Swagger sends it as a Bearer token.")));
    }
}
