package com.web.volunteer.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats {
    private Long totalEvents;
    private Long upcomingEvents;
    private Long myRegistrations;
    private Long completedEvents;
    private Long pendingApprovals;
}
