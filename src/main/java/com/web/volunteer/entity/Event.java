package com.web.volunteer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic fields
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String location;

    // Category (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // Event dates
    @Column(nullable = false)
    private LocalDateTime eventDate;

    private LocalDateTime registrationDeadline;

    private Integer maxParticipants;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    // Creator
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    // Approved admin info
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // Registrations
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<EventRegistration> registrations;

    // Posts
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<Post> posts;

    // Audit fields
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Business: Can user register?
     */
    public boolean canRegister() {
        boolean beforeDeadline = registrationDeadline == null ||
                LocalDateTime.now().isBefore(registrationDeadline);

        boolean slotAvailable = maxParticipants == null ||
                registrations.stream().filter(r -> r.getStatus() ==
                        EventRegistration.RegistrationStatus.APPROVED).count() < maxParticipants;

        return status == EventStatus.APPROVED && beforeDeadline && slotAvailable;
    }

    public enum EventStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
