package com.sliit.smartcampus.entity;

import com.sliit.smartcampus.entity.enums.BookingStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    private String id;

    @Indexed
    private String resourceId;

    @Indexed
    private String userId;

    private Instant startTime;
    private Instant endTime;

    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    private String purpose;
    // Used by administrators when approving/rejecting a booking request.
    private String decisionReason;
    private Instant createdAt;

    public void touchCreated() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
