package com.web.volunteer.controller;

import com.web.volunteer.dto.response.ApiResponse;
import com.web.volunteer.dto.response.DashboardResponse;
import com.web.volunteer.dto.response.DashboardStats;
import com.web.volunteer.dto.response.EventResponse;
import com.web.volunteer.security.SecurityUtils;
import com.web.volunteer.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Dashboard and analytics endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard", description = "Get role-based dashboard data")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse response = dashboardService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(response, "Dashboard data retrieved successfully"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get statistics", description = "Get dashboard statistics only")
    public ResponseEntity<ApiResponse<DashboardStats>> getStats() {
        DashboardStats stats = dashboardService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistics retrieved successfully"));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending events", description = "Get trending events by registration growth")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getTrendingEvents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        Long userId = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // Public access allowed
        }

        List<EventResponse> events = dashboardService.getTrendingEvents(limit, userId);
        return ResponseEntity.ok(ApiResponse.success(events, "Trending events retrieved successfully"));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active events", description = "Get recently active events by post activity")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getRecentlyActiveEvents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        Long userId = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // Public access allowed
        }

        List<EventResponse> events = dashboardService.getRecentlyActiveEvents(limit, userId);
        return ResponseEntity.ok(ApiResponse.success(events, "Recently active events retrieved successfully"));
    }
}