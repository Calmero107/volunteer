package com.web.volunteer.service;
import com.web.volunteer.dto.response.EventResponse;
import com.web.volunteer.dto.response.PageResponse;
import com.web.volunteer.dto.response.RegistrationResponse;
import com.web.volunteer.dto.response.UserResponse;
import com.web.volunteer.entity.Event;
import com.web.volunteer.entity.EventRegistration;
import com.web.volunteer.entity.User;
import com.web.volunteer.exception.BadRequestException;
import com.web.volunteer.exception.ForbiddenException;
import com.web.volunteer.exception.ResourceNotFoundException;
import com.web.volunteer.repository.EventRegistrationRepository;
import com.web.volunteer.repository.EventRepository;
import com.web.volunteer.repository.UserRepository;
import com.web.volunteer.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    /**
     * Register for an event
     */
    @Transactional
    public RegistrationResponse registerForEvent(Long eventId, String notes) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("User {} attempting to register for event {}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Validation checks
        validateRegistration(user, event);

        // Check if already registered
        if (registrationRepository.existsByUserAndEvent(user, event)) {
            throw new BadRequestException("You are already registered for this event");
        }

        // Create registration
        EventRegistration registration = EventRegistration.builder()
                .user(user)
                .event(event)
                .status(EventRegistration.RegistrationStatus.PENDING)
                .notes(notes)
                .completed(false)
                .build();

        registration = registrationRepository.save(registration);
        logger.info("User {} successfully registered for event {}", userId, eventId);

        return mapToRegistrationResponse(registration);
    }

    /**
     * Unregister from an event
     */
    @Transactional
    public void unregisterFromEvent(Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("User {} attempting to unregister from event {}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        EventRegistration registration = registrationRepository.findByUserAndEvent(user, event)
                .orElseThrow(() -> new BadRequestException("You are not registered for this event"));

        // Cannot unregister if event has already started
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot unregister after event has started");
        }

        // Cannot unregister if marked as completed
        if (registration.isCompleted()) {
            throw new BadRequestException("Cannot unregister from completed event");
        }

        registrationRepository.delete(registration);
        logger.info("User {} successfully unregistered from event {}", userId, eventId);
    }

    /**
     * Get my registrations
     */
    @Transactional(readOnly = true)
    public PageResponse<RegistrationResponse> getMyRegistrations(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("Fetching registrations for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Page<EventRegistration> registrationPage = registrationRepository.findByUser(user, pageable);
        Page<RegistrationResponse> responsePage = registrationPage.map(this::mapToRegistrationResponse);

        return buildPageResponse(responsePage);
    }

    /**
     * Get registrations for an event (EVENT_MANAGER/ADMIN only)
     */
    @Transactional(readOnly = true)
    public PageResponse<RegistrationResponse> getEventRegistrations(
            Long eventId,
            EventRegistration.RegistrationStatus status,
            Pageable pageable
    ) {
        logger.info("Fetching registrations for event {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Check if user has permission to view registrations
        Long userId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.isAdmin() && !event.getCreator().getId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to view registrations for this event");
        }

        Page<EventRegistration> registrationPage;
        if (status != null) {
            registrationPage = registrationRepository.findByEventAndStatus(event, status, pageable);
        } else {
            registrationPage = registrationRepository.findByEvent(event, pageable);
        }

        Page<RegistrationResponse> responsePage = registrationPage.map(this::mapToRegistrationResponse);

        return buildPageResponse(responsePage);
    }

    /**
     * Approve registration (EVENT_MANAGER/ADMIN only)
     */
    @Transactional
    public RegistrationResponse approveRegistration(Long registrationId) {
        logger.info("Approving registration {}", registrationId);

        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", "id", registrationId));

        // Check permissions
        validateEventManagementPermission(registration.getEvent());

        if (registration.getStatus() == EventRegistration.RegistrationStatus.APPROVED) {
            throw new BadRequestException("Registration is already approved");
        }

        // Check if event is full
        Event event = registration.getEvent();
        if (event.getMaxParticipants() != null) {
            long approvedCount = registrationRepository.countApprovedRegistrationsByEvent(event);
            if (approvedCount >= event.getMaxParticipants()) {
                throw new BadRequestException("Event has reached maximum participants");
            }
        }

        registration.setStatus(EventRegistration.RegistrationStatus.APPROVED);
        registration = registrationRepository.save(registration);

        logger.info("Registration {} approved successfully", registrationId);
        return mapToRegistrationResponse(registration);
    }

    /**
     * Reject registration (EVENT_MANAGER/ADMIN only)
     */
    @Transactional
    public RegistrationResponse rejectRegistration(Long registrationId) {
        logger.info("Rejecting registration {}", registrationId);

        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", "id", registrationId));

        // Check permissions
        validateEventManagementPermission(registration.getEvent());

        registration.setStatus(EventRegistration.RegistrationStatus.REJECTED);
        registration = registrationRepository.save(registration);

        logger.info("Registration {} rejected", registrationId);
        return mapToRegistrationResponse(registration);
    }

    /**
     * Mark registration as completed (EVENT_MANAGER/ADMIN only)
     */
    @Transactional
    public RegistrationResponse markAsCompleted(Long registrationId) {
        logger.info("Marking registration {} as completed", registrationId);

        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", "id", registrationId));

        // Check permissions
        validateEventManagementPermission(registration.getEvent());

        if (registration.getStatus() != EventRegistration.RegistrationStatus.APPROVED) {
            throw new BadRequestException("Only approved registrations can be marked as completed");
        }

        if (registration.isCompleted()) {
            throw new BadRequestException("Registration is already marked as completed");
        }

        registration.setCompleted(true);
        registration.setCompletedAt(LocalDateTime.now());
        registration = registrationRepository.save(registration);

        logger.info("Registration {} marked as completed", registrationId);
        return mapToRegistrationResponse(registration);
    }

    /**
     * Get registration history for current user
     */
    @Transactional(readOnly = true)
    public PageResponse<RegistrationResponse> getRegistrationHistory(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("Fetching registration history for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Page<EventRegistration> historyPage = registrationRepository.findUserRegistrationHistory(user, pageable);
        Page<RegistrationResponse> responsePage = historyPage.map(this::mapToRegistrationResponse);

        return buildPageResponse(responsePage);
    }

    // ========== Private Helper Methods ==========

    /**
     * Validate registration eligibility
     */
    private void validateRegistration(User user, Event event) {
        // Event must be approved
        if (event.getApprovedAt() == null) {
            throw new BadRequestException("Event is not approved yet");
        }

        // Check registration deadline
        if (!event.canRegister()) {
            throw new BadRequestException("Registration deadline has passed or event is full");
        }

        // Check if event has already ended
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot register for past events");
        }

        // Check max participants
        if (event.getMaxParticipants() != null) {
            long approvedCount = registrationRepository.countApprovedRegistrationsByEvent(event);
            if (approvedCount >= event.getMaxParticipants()) {
                throw new BadRequestException("Event has reached maximum participants");
            }
        }
    }

    /**
     * Validate if user can manage event registrations
     */
    private void validateEventManagementPermission(Event event) {
        Long userId = SecurityUtils.getCurrentUserId();

        if (!SecurityUtils.isAdmin() && !event.getCreator().getId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to manage registrations for this event");
        }
    }

    /**
     * Map EventRegistration to RegistrationResponse
     */
    private RegistrationResponse mapToRegistrationResponse(EventRegistration registration) {
        return RegistrationResponse.builder()
                .id(registration.getId())
                .user(mapToUserResponse(registration.getUser()))
                .event(mapToEventResponse(registration.getEvent()))
                .status(registration.getStatus().name())
                .notes(registration.getNotes())
                .completed(registration.isCompleted())
                .completedAt(registration.getCompletedAt())
                .registeredAt(registration.getRegisteredAt())
                .updatedAt(registration.getUpdatedAt())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .build();
    }

    private EventResponse mapToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .status(event.getStatus().name())
                .build();
    }

    private <T> PageResponse<T> buildPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
