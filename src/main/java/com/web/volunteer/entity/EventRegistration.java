package com.web.volunteer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_registrations")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    // Ghi chú của admin hoặc user
    @Column(columnDefinition = "TEXT")
    private String notes;

    // --- Thời điểm user đăng ký ---
    @Column(nullable = false)
    private LocalDateTime registeredAt;

    // --- Thời điểm update ---
    private LocalDateTime updatedAt;

    // --- Đánh dấu hoàn thành nhiệm vụ ---
    @Column(nullable = false)
    private boolean completed = false;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
    }

    public enum RegistrationStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }
}
