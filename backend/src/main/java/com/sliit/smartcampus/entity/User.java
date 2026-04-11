package com.sliit.smartcampus.entity;

import com.sliit.smartcampus.entity.enums.UserRole;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;

    @Builder.Default
    private UserRole role = UserRole.USER;

    private String passwordHash;

    /** Set when user authenticated via Google OAuth2. */
    private String googleId;

    /** Profile picture URL from Google. */
    private String picture;

    private Instant createdAt;

    /**
     * Initialize the creation timestamp when a user record is first saved.
     */
    public void touchCreated() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
