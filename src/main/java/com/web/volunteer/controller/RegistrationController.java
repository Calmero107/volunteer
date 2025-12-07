package com.web.volunteer.controller;

import com.web.volunteer.dto.response.ApiResponse;
import com.web.volunteer.dto.response.PageResponse;
import com.web.volunteer.dto.response.RegistrationResponse;
import com.web.volunteer.entity.EventRegistration;
import com.web.volunteer.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Registrations", description = "Event registration management endpoints")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/events/{eventId}/register")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'EVENT_MANAGER', 'ADMIN')")
    @Operation(summary = "Register for event", description = "Register current user for an event")
    public ResponseEntity<ApiResponse<RegistrationResponse>> registerForEvent(
            @PathVariable Long eventId,
            @RequestParam(required = false) String notes
    ) {
        RegistrationResponse response = registrationService.registerForEvent(eventId, notes);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registered for event successfully"));
    }

    @DeleteMapping("/events/{eventId}/unregister")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'EVENT_MANAGER', 'ADMIN')")
    @Operation(summary = "Unregister from event", description = "Cancel registration for an event")
    public ResponseEntity<ApiResponse<Void>> unregisterFromEvent(@PathVariable Long eventId) {
        registrationService.unregisterFromEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(null, "Unregistered from event successfully"));
    }

    @GetMapping("/registrations/my")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'EVENT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get my registrations", description = "Get all registrations for current user")
    public ResponseEntity<ApiResponse<PageResponse<RegistrationResponse>>> getMyRegistrations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").descending());
        PageResponse<RegistrationResponse> response = registrationService.getMyRegistrations(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Registrations retrieved successfully"));
    }

    @GetMapping("/registrations/history")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'EVENT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get registration history", description = "Get registration history with completed events")
    public ResponseEntity<ApiResponse<PageResponse<RegistrationResponse>>> getRegistrationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<RegistrationResponse> response = registrationService.getRegistrationHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Registration history retrieved successfully"));
    }

    @GetMapping("/events/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('EVENT_MANAGER', 'ADMIN')")
    @Operation(summary = "Get event registrations", description = "Get all registrations for an event (EVENT_MANAGER/ADMIN)")
    public ResponseEntity<ApiResponse<PageResponse<RegistrationResponse>>> getEventRegistrations(
            @PathVariable Long eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").descending());
        EventRegistration.RegistrationStatus regStatus = null;
        if (status != null && !status.isEmpty()) {
            regStatus = EventRegistration.RegistrationStatus.valueOf(status.toUpperCase());
        }

        PageResponse<RegistrationResponse> response = registrationService.getEventRegistrations(
                eventId, regStatus, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Event registrations retrieved successfully"));
    }

    @PatchMapping("/registrations/{id}/approve")
    @PreAuthorize("hasAnyRole('EVENT_MANAGER', 'ADMIN')")
    @Operation(summary = "Approve registration", description = "Approve a pending registration (EVENT_MANAGER/ADMIN)")
    public ResponseEntity<ApiResponse<RegistrationResponse>> approveRegistration(@PathVariable Long id) {
        RegistrationResponse response = registrationService.approveRegistration(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Registration approved successfully"));
    }

    @PatchMapping("/registrations/{id}/reject")
    @PreAuthorize("hasAnyRole('EVENT_MANAGER', 'ADMIN')")
    @Operation(summary = "Reject registration", description = "Reject a pending registration (EVENT_MANAGER/ADMIN)")
    public ResponseEntity<ApiResponse<RegistrationResponse>> rejectRegistration(@PathVariable Long id) {
        RegistrationResponse response = registrationService.rejectRegistration(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Registration rejected successfully"));
    }

    @PatchMapping("/registrations/{id}/complete")
    @PreAuthorize("hasAnyRole('EVENT_MANAGER', 'ADMIN')")
    @Operation(summary = "Mark as completed", description = "Mark registration as completed (EVENT_MANAGER/ADMIN)")
    public ResponseEntity<ApiResponse<RegistrationResponse>> markAsCompleted(@PathVariable Long id) {
        RegistrationResponse response = registrationService.markAsCompleted(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Registration marked as completed"));
    }
}
