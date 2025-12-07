package com.web.volunteer.service;

import com.web.volunteer.dto.response.PageResponse;
import com.web.volunteer.dto.response.UserResponse;
import com.web.volunteer.dto.response.UserStats;
import com.web.volunteer.entity.User;
import com.web.volunteer.enums.Role;
import com.web.volunteer.exception.BadRequestException;
import com.web.volunteer.exception.ResourceNotFoundException;
import com.web.volunteer.repository.EventRepository;
import com.web.volunteer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    /**
     * Get all users with pagination
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Role role, String search, Pageable pageable) {
        logger.info("Fetching all users - role: {}, search: {}", role, search);

        Page<User> userPage;

        if (role != null) {
            userPage = userRepository.findByRole(role, pageable);
        } else if (search != null && !search.isEmpty()) {
            // This would require adding a search method to UserRepository
            // For now, get all users
            userPage = userRepository.findAll(pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        Page<UserResponse> responsePage = userPage.map(this::mapToUserResponse);

        return PageResponse.<UserResponse>builder()
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
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        logger.info("Fetching user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return mapToUserResponse(user);
    }

    /**
     * Lock user account
     */
    @Transactional
    public UserResponse lockUser(Long userId) {
        logger.info("Locking user account: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot lock admin accounts");
        }

        if (user.isLocked()) {
            throw new BadRequestException("User account is already locked");
        }

        user.setLocked(true);
        user = userRepository.save(user);

        logger.info("User account {} locked successfully", userId);
        return mapToUserResponse(user);
    }

    /**
     * Unlock user account
     */
    @Transactional
    public UserResponse unlockUser(Long userId) {
        logger.info("Unlocking user account: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!user.isLocked()) {
            throw new BadRequestException("User account is not locked");
        }

        user.setLocked(false);
        user = userRepository.save(user);

        logger.info("User account {} unlocked successfully", userId);
        return mapToUserResponse(user);
    }

    /**
     * Deactivate user account
     */
    @Transactional
    public UserResponse deactivateUser(Long userId) {
        logger.info("Deactivating user account: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot deactivate admin accounts");
        }

        user.setActive(false);
        user = userRepository.save(user);

        logger.info("User account {} deactivated successfully", userId);
        return mapToUserResponse(user);
    }

    /**
     * Activate user account
     */
    @Transactional
    public UserResponse activateUser(Long userId) {
        logger.info("Activating user account: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setActive(true);
        user = userRepository.save(user);

        logger.info("User account {} activated successfully", userId);
        return mapToUserResponse(user);
    }

    /**
     * Delete user account (soft delete - just deactivate)
     */
    @Transactional
    public void deleteUser(Long userId) {
        logger.info("Deleting user account: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot delete admin accounts");
        }

        // Check if user has created events
        long eventCount = eventRepository.findByCreator(user, Pageable.unpaged()).getTotalElements();
        if (eventCount > 0) {
            throw new BadRequestException("Cannot delete user with created events. Deactivate instead.");
        }

        // For safety, we deactivate instead of hard delete
        user.setActive(false);
        user.setLocked(true);
        userRepository.save(user);

        logger.info("User account {} deleted (deactivated) successfully", userId);
    }

    /**
     * Get user statistics
     */
    @Transactional(readOnly = true)
    public UserStats getUserStats() {
        logger.info("Fetching user statistics");

        long totalUsers = userRepository.count();
        long volunteers = userRepository.countByRole(Role.VOLUNTEER);
        long eventManagers = userRepository.countByRole(Role.ORGANIZER);
        long admins = userRepository.countByRole(Role.ADMIN);

        return UserStats.builder()
                .totalUsers(totalUsers)
                .volunteers(volunteers)
                .eventManagers(eventManagers)
                .admins(admins)
                .build();
    }

    // ========== Private Helper Methods ==========

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .active(user.isActive())
                .locked(user.isLocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
