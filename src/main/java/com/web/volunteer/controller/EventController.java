package com.web.volunteer.controller;

import com.web.volunteer.dto.request.CreateEventRequest;
import com.web.volunteer.dto.request.UpdateEventRequest;
import com.web.volunteer.dto.response.ApiResponse;
import com.web.volunteer.dto.response.EventResponse;
import com.web.volunteer.dto.response.PageResponse;
import com.web.volunteer.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Get all events", description = "Get all events with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> getAllEvents(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<EventResponse> response = eventService.getAllEvents(
                categoryId, search, startDate, endDate, status, page, size
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Events retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Get detailed information about a specific event")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable Long id) {
        EventResponse response = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Event retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create event", description = "Create a new event (ORGANIZER or ADMIN)")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request
    ) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Event created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update event", description = "Update an existing event (creator or ADMIN)")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        EventResponse response = eventService.updateEvent(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Event updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete event", description = "Delete an event (creator or ADMIN)")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Event deleted successfully"));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Approve event", description = "Approve a pending event (ADMIN only)")
    public ResponseEntity<ApiResponse<EventResponse>> approveEvent(@PathVariable Long id) {
        EventResponse response = eventService.approveEvent(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Event approved successfully"));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reject event", description = "Reject a pending event (ADMIN only)")
    public ResponseEntity<ApiResponse<EventResponse>> rejectEvent(@PathVariable Long id) {
        EventResponse response = eventService.rejectEvent(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Event rejected successfully"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my events", description = "Get events created by current user")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<EventResponse> response = eventService.getMyEvents(page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "My events retrieved successfully"));
    }
}