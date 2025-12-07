package com.web.volunteer.repository;

import com.web.volunteer.entity.Event;
import com.web.volunteer.entity.EventRegistration;
import com.web.volunteer.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    Optional<EventRegistration> findByUserAndEvent(User user, Event event);

    boolean existsByUserAndEvent(User user, Event event);

    Page<EventRegistration> findByUser(User user, Pageable pageable);

    Page<EventRegistration> findByEvent(Event event, Pageable pageable);

    Page<EventRegistration> findByEventAndStatus(Event event, EventRegistration.RegistrationStatus status, Pageable pageable);

    @Query("SELECT r FROM EventRegistration r WHERE r.user = :user " +
            "AND r.status = 'APPROVED' " +
            "ORDER BY r.event.eventDate DESC")
    Page<EventRegistration> findUserRegistrationHistory(@Param("user") User user, Pageable pageable);

    @Query("SELECT COUNT(r) FROM EventRegistration r WHERE r.event = :event AND r.status = 'APPROVED'")
    long countApprovedRegistrationsByEvent(@Param("event") Event event);

    @Query("SELECT r FROM EventRegistration r WHERE r.user = :user " +
            "AND r.event.eventDate >= CURRENT_TIMESTAMP " +
            "AND r.status = 'APPROVED'")
    List<EventRegistration> findUpcomingRegistrationsByUser(@Param("user") User user);
}
