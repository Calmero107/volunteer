package com.web.volunteer.controller;

import com.web.volunteer.dto.response.ApiResponse;
import com.web.volunteer.dto.response.PageResponse;
import com.web.volunteer.dto.response.UserResponse;
import com.web.volunteer.dto.response.UserStats;
import com.web.volunteer.enums.Role;
import com.web.volunteer.service.AdminService;
import com.web.volunteer.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin management endpoints (ADMIN only)")
public class AdminController {

    private final AdminService adminService;
    private final ExportService exportService;

    // ========== User Management ==========

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Get all users with pagination and filters")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Role userRole = null;
        if (role != null && !role.isEmpty()) {
            userRole = Role.valueOf(role.toUpperCase());
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<UserResponse> response = adminService.getAllUsers(userRole, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Users retrieved successfully"));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID", description = "Get detailed user information")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User retrieved successfully"));
    }

    @PatchMapping("/users/{id}/lock")
    @Operation(summary = "Lock user", description = "Lock user account (prevent login)")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(@PathVariable Long id) {
        UserResponse response = adminService.lockUser(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User locked successfully"));
    }

    @PatchMapping("/users/{id}/unlock")
    @Operation(summary = "Unlock user", description = "Unlock user account")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable Long id) {
        UserResponse response = adminService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User unlocked successfully"));
    }

    @PatchMapping("/users/{id}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivate user account")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable Long id) {
        UserResponse response = adminService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User deactivated successfully"));
    }

    @PatchMapping("/users/{id}/activate")
    @Operation(summary = "Activate user", description = "Activate user account")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long id) {
        UserResponse response = adminService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success(response, "User activated successfully"));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user", description = "Delete user account (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @GetMapping("/users/stats")
    @Operation(summary = "Get user statistics", description = "Get user statistics by role")
    public ResponseEntity<ApiResponse<UserStats>> getUserStats() {
        UserStats stats = adminService.getUserStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "User statistics retrieved successfully"));
    }

    // ========== Export Data ==========

    @GetMapping("/export/events/csv")
    @Operation(summary = "Export events to CSV", description = "Export all events to CSV format")
    public ResponseEntity<String> exportEventsToCSV() throws IOException {
        String csv = exportService.exportEventsToCSV();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "events.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    @GetMapping("/export/events/json")
    @Operation(summary = "Export events to JSON", description = "Export all events to JSON format")
    public ResponseEntity<String> exportEventsToJSON() throws IOException {
        String json = exportService.exportEventsToJSON();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "events.json");

        return ResponseEntity.ok()
                .headers(headers)
                .body(json);
    }

    @GetMapping("/export/users/csv")
    @Operation(summary = "Export users to CSV", description = "Export all users to CSV format")
    public ResponseEntity<String> exportUsersToCSV() throws IOException {
        String csv = exportService.exportUsersToCSV();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "users.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    @GetMapping("/export/users/json")
    @Operation(summary = "Export users to JSON", description = "Export all users to JSON format")
    public ResponseEntity<String> exportUsersToJSON() throws IOException {
        String json = exportService.exportUsersToJSON();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "users.json");

        return ResponseEntity.ok()
                .headers(headers)
                .body(json);
    }
}
