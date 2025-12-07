package com.web.volunteer.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationResponse {

    private Long id;
    private UserResponse user;
    private EventResponse event;
    private String status;
    private String notes;
    private Boolean completed;
    private LocalDateTime completedAt;
    private LocalDateTime registeredAt;
    private LocalDateTime updatedAt;
}
