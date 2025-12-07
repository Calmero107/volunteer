package com.web.volunteer.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private DashboardStats stats;
    private List<EventResponse> upcomingEvents;
    private List<EventResponse> trendingEvents;
    private List<EventResponse> recentlyActiveEvents;
    private List<PostResponse> recentPosts;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class DashboardStats {
    private Long totalEvents;
    private Long upcomingEvents;
    private Long myRegistrations;
    private Long completedEvents;
    private Long pendingApprovals;
}

