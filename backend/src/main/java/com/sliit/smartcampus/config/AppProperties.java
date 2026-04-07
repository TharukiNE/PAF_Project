package com.sliit.smartcampus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private String adminEmails = "";
    private String uploadDir = "uploads";
    private String frontendUrl = "http://localhost:5173";

    public List<String> adminEmailList() {
        if (adminEmails == null || adminEmails.isBlank()) {
            return List.of();
        }
        return Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }

    @Getter
    @Setter
    public static class Jwt {
        /** HS256 signing secret — must be at least 32 characters. */
        private String secret = "";
        private long expirationSeconds = 86400;
    }
}
