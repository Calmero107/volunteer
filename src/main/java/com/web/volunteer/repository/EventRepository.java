package com.web.volunteer.repository;

import com.web.volunteer.entity.Event;
import com.web.volunteer.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByStatus(Event.EventStatus status, Pageable pageable);

    Page<Event> findByCreator(User creator, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'APPROVED' " +
            "AND e.eventDate >= :startDate " +
            "ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingApprovedEvents(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'APPROVED' " +
            "AND (:categoryId IS NULL OR e.category.id = :categoryId) " +
            "AND (:searchTerm IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND (:startDate IS NULL OR e.eventDate >= :startDate) " +
            "AND (:endDate IS NULL OR e.eventDate <= :endDate) " +
            "ORDER BY e.eventDate ASC")
    Page<Event> findEventsWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("searchTerm") String searchTerm,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN e.registrations r " +
            "WHERE e.status = 'APPROVED' " +
            "AND e.eventDate >= :since " +
            "GROUP BY e.id " +
            "ORDER BY COUNT(r) DESC")
    List<Event> findTrendingEvents(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN e.posts p " +
            "WHERE e.status = 'APPROVED' " +
            "AND p.createdAt >= :since " +
            "GROUP BY e.id " +
            "ORDER BY COUNT(p) DESC")
    List<Event> findEventsWithRecentActivity(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status")
    long countByStatus(@Param("status") Event.EventStatus status);
}