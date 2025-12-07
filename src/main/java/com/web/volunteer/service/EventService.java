package com.web.volunteer.service;

import com.web.volunteer.dto.request.CreateEventRequest;
import com.web.volunteer.dto.request.UpdateEventRequest;
import com.web.volunteer.dto.response.CategoryResponse;
import com.web.volunteer.dto.response.EventResponse;
import com.web.volunteer.dto.response.PageResponse;
import com.web.volunteer.dto.response.UserResponse;
import com.web.volunteer.entity.Category;
import com.web.volunteer.entity.Event;
import com.web.volunteer.entity.User;
import com.web.volunteer.exception.BadRequestException;
import com.web.volunteer.exception.ForbiddenException;
import com.web.volunteer.exception.ResourceNotFoundException;
import com.web.volunteer.repository.CategoryRepository;
import com.web.volunteer.repository.EventRegistrationRepository;
import com.web.volunteer.repository.EventRepository;
import com.web.volunteer.repository.UserRepository;
import com.web.volunteer.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRegistrationRepository registrationRepository;

    /**
     * Get all events with filters
     */
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> getAllEvents(
            Long categoryId,
            String searchTerm,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String status,
            int page,
            int size
    ) {
        logger.info("Fetching events with filters - categoryId: {}, status: {}", categoryId, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").ascending());

        Page<Event> eventPage;

        if (status != null && !status.isEmpty()) {
            Event.EventStatus eventStatus = Event.EventStatus.valueOf(status.toUpperCase());
            eventPage = eventRepository.findByStatus(eventStatus, pageable);
        } else if (categoryId != null || searchTerm != null || startDate != null || endDate != null) {
            eventPage = eventRepository.findEventsWithFilters(categoryId, searchTerm, startDate, endDate, pageable);
        } else {
            eventPage = eventRepository.findUpcomingApprovedEvents(LocalDateTime.now(), pageable);
        }

        Long currentUserId = null;
        try {
            currentUserId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // User not authenticated, continue without user-specific data
        }

        final Long userId = currentUserId;
        Page<EventResponse> responsePage = eventPage.map(event -> mapToEventResponse(event, userId));

        return PageResponse.<EventResponse>builder()
                .content(responsePage.getContent())
                .pageNumber(responsePage.getNumber())
                .pageSize(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .last(responsePage.isLast())
                .first(responsePage.isFirst())
                .build();
    }

    /**
     * Get event by ID
     */
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId) {
        logger.info("Fetching event with ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        Long currentUserId = null;
        try {
            currentUserId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // User not authenticated
        }

        return mapToEventResponse(event, currentUserId);
    }

    /**
     * Create new event (EVENT_MANAGER only)
     */
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("Creating new event by user ID: {}", userId);

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate dates
        if (request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Event date must be in the future");
        }

        if (request.getRegistrationDeadline() != null &&
                request.getRegistrationDeadline().isAfter(request.getEventDate())) {
            throw new BadRequestException("Registration deadline must be before event date");
        }

        // Get category if provided
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .eventDate(request.getEventDate())
                .registrationDeadline(request.getRegistrationDeadline())
                .maxParticipants(request.getMaxParticipants())
                .status(Event.EventStatus.PENDING)
                .creator(creator)
                .category(category)
                .build();

        event = eventRepository.save(event);
        logger.info("Event created successfully with ID: {}", event.getId());

        return mapToEventResponse(event, userId);
    }

    /**
     * Update event (EVENT_MANAGER - own events only, ADMIN - all events)
     */
    @Transactional
    public EventResponse updateEvent(Long eventId, UpdateEventRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("Updating event ID: {} by user ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Check permissions
        if (!SecurityUtils.isAdmin() && !event.getCreator().getId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to update this event");
        }

        // Can't update approved events unless admin
        if (event.getStatus() == Event.EventStatus.APPROVED && !SecurityUtils.isAdmin()) {
            throw new BadRequestException("Cannot update approved events. Please contact admin.");
        }

        // Update fields
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }
        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Event date must be in the future");
            }
            event.setEventDate(request.getEventDate());
        }
        if (request.getRegistrationDeadline() != null) {
            event.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        if (request.getMaxParticipants() != null) {
            event.setMaxParticipants(request.getMaxParticipants());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            event.setCategory(category);
        }

        event = eventRepository.save(event);
        logger.info("Event updated successfully: {}", eventId);

        return mapToEventResponse(event, userId);
    }

    /**
     * Delete event (EVENT_MANAGER - own events only, ADMIN - all events)
     */
    @Transactional
    public void deleteEvent(Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("Deleting event ID: {} by user ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Check permissions
        if (!SecurityUtils.isAdmin() && !event.getCreator().getId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to delete this event");
        }

        // Can't delete events with registrations unless admin
        long registrationCount = registrationRepository.countApprovedRegistrationsByEvent(event);
        if (registrationCount > 0 && !SecurityUtils.isAdmin()) {
            throw new BadRequestException("Cannot delete events with registrations. Please contact admin.");
        }

        eventRepository.delete(event);
        logger.info("Event deleted successfully: {}", eventId);
    }

    /**
     * Approve event (ADMIN only)
     */
    @Transactional
    public EventResponse approveEvent(Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("Approving event ID: {} by admin ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User admin = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (event.getStatus() == Event.EventStatus.APPROVED) {
            throw new BadRequestException("Event is already approved");
        }

        event.setStatus(Event.EventStatus.APPROVED);
        event.setApprovedAt(LocalDateTime.now());
        event.setApprovedBy(admin);

        event = eventRepository.save(event);
        logger.info("Event approved successfully: {}", eventId);

        return mapToEventResponse(event, userId);
    }

    /**
     * Reject event (ADMIN only)
     */
    @Transactional
    public EventResponse rejectEvent(Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("Rejecting event ID: {} by admin ID: {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        event.setStatus(Event.EventStatus.REJECTED);
        event = eventRepository.save(event);

        logger.info("Event rejected successfully: {}", eventId);
        return mapToEventResponse(event, userId);
    }

    /**
     * Get my created events
     */
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> getMyEvents(int page, int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        logger.info("Fetching events created by user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Event> eventPage = eventRepository.findByCreator(user, pageable);

        Page<EventResponse> responsePage = eventPage.map(event -> mapToEventResponse(event, userId));

        return PageResponse.<EventResponse>builder()
                .content(responsePage.getContent())
                .pageNumber(responsePage.getNumber())
                .pageSize(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .last(responsePage.isLast())
                .first(responsePage.isFirst())
                .build();
    }

    /**
     * Map Event entity to EventResponse DTO
     */
    private EventResponse mapToEventResponse(Event event, Long currentUserId) {
        long currentParticipants = registrationRepository.countApprovedRegistrationsByEvent(event);

        boolean isRegistered = false;
        if (currentUserId != null) {
            User user = userRepository.findById(currentUserId).orElse(null);
            if (user != null) {
                isRegistered = registrationRepository.existsByUserAndEvent(user, event);
            }
        }

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .registrationDeadline(event.getRegistrationDeadline())
                .maxParticipants(event.getMaxParticipants())
                .currentParticipants((int) currentParticipants)
                .status(event.getStatus().name())
                .category(event.getCategory() != null ? mapToCategoryResponse(event.getCategory()) : null)
                .creator(mapToUserResponse(event.getCreator()))
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .approvedAt(event.getApprovedAt())
                .canRegister(event.canRegister())
                .isRegistered(isRegistered)
                .build();
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
