package com.web.volunteer.repository;

import com.web.volunteer.entity.Event;
import com.web.volunteer.entity.Post;
import com.web.volunteer.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByEvent(Event event, Pageable pageable);

    Page<Post> findByAuthor(User author, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.event = :event ORDER BY p.createdAt DESC")
    Page<Post> findByEventOrderByCreatedAtDesc(@Param("event") Event event, Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN p.likes l " +
            "WHERE p.event = :event " +
            "GROUP BY p.id " +
            "ORDER BY COUNT(l) DESC, p.createdAt DESC")
    Page<Post> findByEventOrderByPopularity(@Param("event") Event event, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.event = :event AND p.createdAt >= :since")
    long countRecentPostsByEvent(@Param("event") Event event, @Param("since") LocalDateTime since);
}
