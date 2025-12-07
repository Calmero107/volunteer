package com.web.volunteer.service;

import com.web.volunteer.dto.response.DashboardResponse;
import com.web.volunteer.dto.response.DashboardStats;
import com.web.volunteer.dto.response.EventResponse;
import com.web.volunteer.dto.response.PostResponse;
import com.web.volunteer.entity.Event;
import com.web.volunteer.entity.EventRegistration;
import com.web.volunteer.entity.User;
import com.web.volunteer.exception.ResourceNotFoundException;
import com.web.volunteer.repository.EventRegistrationRepository;
import com.web.volunteer.repository.EventRepository;
import com.web.volunteer.repository.PostRepository;
import com.web.volunteer.repository.UserRepository;
import com.web.volunteer.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    /**
     * Get dashboard data based on user role
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserDetails().getRole();

        logger.info("Fetching dashboard for user {} with role {}", userId, role);

        return switch (role) {
            case "VOLUNTEER" -> getVolunteerDashboard(userId);
            case "EVENT_MANAGER" -> getEventManagerDashboard(userId);
            case "ADMIN" -> getAdminDashboard(userId);
            default -> throw new IllegalStateException("Unknown role: " + role);
        };
    }

    /**
     * Get volunteer dashboard
     */
    private DashboardResponse getVolunteerDashboard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Statistics
        long myRegistrations = registrationRepository.countByUser(user);
        long completedEvents = registrationRepository.findByUser(user, Pageable.unpaged())
                .stream()
                .filter(EventRegistration::isCompleted)
                .count();

        DashboardStats stats = DashboardStats.builder()
                .totalEvents(eventRepository.countByStatus(Event.EventStatus.APPROVED))
                .upcomingEvents(countUpcomingEvents())
                .myRegistrations(myRegistrations)
                .completedEvents(completedEvents)
                .build();

        // Upcoming events user is registered for
        List<EventRegistration> upcomingRegs = registrationRepository
                .findUpcomingRegistrationsByUser(user);
        List<EventResponse> upcomingEvents = upcomingRegs.stream()
                .map(reg -> mapToEventResponse(reg.getEvent(), userId))
                .limit(5)
                .collect(Collectors.toList());

        // Trending events
        List<EventResponse> trending = getTrendingEvents(5, userId);

        // Recently active events
        List<EventResponse> recentlyActive = getRecentlyActiveEvents(5, userId);

        return DashboardResponse.builder()
                .stats(stats)
                .upcomingEvents(upcomingEvents)
                .trendingEvents(trending)
                .recentlyActiveEvents(recentlyActive)
                .build();
    }

    /**
     * Get event manager dashboard
     */
    private DashboardResponse getEventManagerDashboard(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Statistics
        Page<Event> myEvents = eventRepository.findByCreator(user, Pageable.unpaged());
        long pendingApprovals = myEvents.stream()
                .mapToLong(event -> registrationRepository.findByEventAndStatus(
                        event,
                        EventRegistration.RegistrationStatus.PENDING,
                        Pageable.unpaged()
                ).getTotalElements())
                .sum();

        long myApprovedEvents = myEvents.stream()
                .filter(event -> event.getStatus() == Event.EventStatus.APPROVED)
                .count();

        DashboardStats stats = DashboardStats.builder()
                .totalEvents(myEvents.getTotalElements())
                .upcomingEvents(countMyUpcomingEvents(user))
                .myRegistrations(0L) // Not applicable for event managers
                .pendingApprovals(pendingApprovals)
                .completedEvents(countMyCompletedEvents(user))
                .build();

        // My upcoming events
        List<EventResponse> upcomingEvents = myEvents.getContent().stream()
                .filter(event -> event.getStatus() == Event.EventStatus.APPROVED)
                .filter(event -> event.getEventDate().isAfter(LocalDateTime.now()))
                .sorted((e1, e2) -> e1.getEventDate().compareTo(e2.getEventDate()))
                .limit(5)
                .map(event -> mapToEventResponse(event, userId))
                .collect(Collectors.toList());

        // Trending events
        List<EventResponse> trending = getTrendingEvents(5, userId);

        // Recently active events (with new posts)
        List<EventResponse> recentlyActive = getRecentlyActiveEvents(5, userId);

        return DashboardResponse.builder()
                .stats(stats)
                .upcomingEvents(upcomingEvents)
                .trendingEvents(trending)
                .recentlyActiveEvents(recentlyActive)
                .build();
    }

    /**
     * Get admin dashboard
     */
    private DashboardResponse getAdminDashboard(Long userId) {
        // Global statistics
        long totalEvents = eventRepository.count();
        long pendingEvents = eventRepository.countByStatus(Event.EventStatus.PENDING);
        long approvedEvents = eventRepository.countByStatus(Event.EventStatus.APPROVED);
        long totalUsers = userRepository.count();

        DashboardStats stats = DashboardStats.builder()
                .totalEvents(totalEvents)
                .upcomingEvents(countUpcomingEvents())
                .pendingApprovals(pendingEvents)
                .completedEvents(eventRepository.countByStatus(Event.EventStatus.COMPLETED))
                .build();

        // Pending events for approval
        List<Event> pendingEventsList = eventRepository
                .findByStatus(Event.EventStatus.PENDING, PageRequest.of(0, 5))
                .getContent();
        List<EventResponse> pendingEventsResponse = pendingEventsList.stream()
                .map(event -> mapToEventResponse(event, userId))
                .collect(Collectors.toList());

        // Trending events
        List<EventResponse> trending = getTrendingEvents(5, userId);

        // Recently active events
        List<EventResponse> recentlyActive = getRecentlyActiveEvents(5, userId);

        return DashboardResponse.builder()
                .stats(stats)
                .upcomingEvents(pendingEventsResponse) // Show pending events for admin
                .trendingEvents(trending)
                .recentlyActiveEvents(recentlyActive)
                .build();
    }

    /**
     * Get trending events (by registration growth)
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getTrendingEvents(int limit, Long currentUserId) {
        logger.info("Fetching trending events");

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Pageable pageable = PageRequest.of(0, limit);

        List<Event> trendingEvents = eventRepository.findTrendingEvents(since, pageable);

        return trendingEvents.stream()
                .map(event -> mapToEventResponse(event, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Get recently active events (by post activity)
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getRecentlyActiveEvents(int limit, Long currentUserId) {
        logger.info("Fetching recently active events");

        LocalDateTime since = LocalDateTime.now().minusDays(3);
        Pageable pageable = PageRequest.of(0, limit);

        List<Event> activeEvents = eventRepository.findEventsWithRecentActivity(since, pageable);

        return activeEvents.stream()
                .map(event -> mapToEventResponse(event, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Get recent posts across all events
     */
    @Transactional(readOnly = true)
    public List<PostResponse> getRecentPosts(int limit) {
        logger.info("Fetching recent posts");

        // This would require adding a method to PostRepository
        // For now, returning empty list
        return List.of();
    }

    /**
     * Get dashboard statistics for specific user
     */
    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserDetails().getRole();

        return switch (role) {
            case "VOLUNTEER" -> getVolunteerStats(userId);
            case "EVENT_MANAGER" -> getEventManagerStats(userId);
            case "ADMIN" -> getAdminStats();
            default -> throw new IllegalStateException("Unknown role: " + role);
        };
    }

    private DashboardStats getVolunteerStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        long myRegistrations = registrationRepository.countByUser(user);
        long completedEvents = registrationRepository.findByUser(user, Pageable.unpaged())
                .stream()
                .filter(EventRegistration::isCompleted)
                .count();

        return DashboardStats.builder()
                .totalEvents(eventRepository.countByStatus(Event.EventStatus.APPROVED))
                .upcomingEvents(countUpcomingEvents())
                .myRegistrations(myRegistrations)
                .completedEvents(completedEvents)
                .build();
    }

    private DashboardStats getEventManagerStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        long totalMyEvents = eventRepository.findByCreator(user, Pageable.unpaged()).getTotalElements();
        long pendingApprovals = eventRepository.findByCreator(user, Pageable.unpaged())
                .stream()
                .mapToLong(event -> registrationRepository.findByEventAndStatus(
                        event,
                        EventRegistration.RegistrationStatus.PENDING,
                        Pageable.unpaged()
                ).getTotalElements())
                .sum();

        return DashboardStats.builder()
                .totalEvents(totalMyEvents)
                .upcomingEvents(countMyUpcomingEvents(user))
                .pendingApprovals(pendingApprovals)
                .completedEvents(countMyCompletedEvents(user))
                .build();
    }

    private DashboardStats getAdminStats() {
        return DashboardStats.builder()
                .totalEvents(eventRepository.count())
                .upcomingEvents(countUpcomingEvents())
                .pendingApprovals(eventRepository.countByStatus(Event.EventStatus.PENDING))
                .completedEvents(eventRepository.countByStatus(Event.EventStatus.COMPLETED))
                .build();
    }

    // ========== Private Helper Methods ==========

    private long countUpcomingEvents() {
        return eventRepository.findUpcomingApprovedEvents(
                LocalDateTime.now(),
                Pageable.unpaged()
        ).getTotalElements();
    }

    private long countMyUpcomingEvents(User user) {
        return eventRepository.findByCreator(user, Pageable.unpaged())
                .stream()
                .filter(event -> event.getStatus() == Event.EventStatus.APPROVED)
                .filter(event -> event.getEventDate().isAfter(LocalDateTime.now()))
                .count();
    }

    private long countMyCompletedEvents(User user) {
        return eventRepository.findByCreator(user, Pageable.unpaged())
                .stream()
                .filter(event -> event.getStatus() == Event.EventStatus.COMPLETED)
                .count();
    }

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
                .createdAt(event.getCreatedAt())
                .canRegister(event.canRegister())
                .isRegistered(isRegistered)
                .build();
    }
}
