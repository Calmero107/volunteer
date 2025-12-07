package com.web.volunteer.service;

import com.web.volunteer.dto.request.LoginRequest;
import com.web.volunteer.dto.request.RegisterRequest;
import com.web.volunteer.dto.response.AuthResponse;
import com.web.volunteer.dto.response.UserResponse;
import com.web.volunteer.entity.RefreshToken;
import com.web.volunteer.entity.User;
import com.web.volunteer.enums.Role;
import com.web.volunteer.exception.BadRequestException;
import com.web.volunteer.exception.ResourceNotFoundException;
import com.web.volunteer.exception.UnauthorizedException;
import com.web.volunteer.repository.RefreshTokenRepository;
import com.web.volunteer.repository.UserRepository;
import com.web.volunteer.security.CustomUserDetails;
import com.web.volunteer.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address already in use");
        }

        // Create new user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.valueOf(request.getRole()))
                .active(true)
                .locked(false)
                .build();

        user = userRepository.save(user);
        logger.info("User registered successfully with ID: {}", user.getId());

        // Generate tokens
        String accessToken = tokenProvider.generateAccessTokenFromUserId(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshTokenStr = tokenProvider.generateRefreshToken();

        // Save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * Login user
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        logger.info("Attempting login for user: {}", request.getEmail());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Check if account is locked
        if (!userDetails.isAccountNonLocked()) {
            throw new UnauthorizedException("Account is locked. Please contact administrator.");
        }

        // Check if account is active
        if (!userDetails.isEnabled()) {
            throw new UnauthorizedException("Account is inactive. Please contact administrator.");
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshTokenStr = tokenProvider.generateRefreshToken();

        // Delete old refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // Save new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        logger.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * Refresh access token
     */
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        logger.info("Attempting to refresh token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Check if token is expired or revoked
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        if (refreshToken.getRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        User user = refreshToken.getUser();

        // Generate new access token
        String accessToken = tokenProvider.generateAccessTokenFromUserId(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        logger.info("Token refreshed successfully for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * Logout user (revoke refresh token)
     */
    @Transactional
    public void logout(String refreshTokenStr) {
        logger.info("Attempting logout");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElse(null);

        if (refreshToken != null) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            logger.info("User logged out successfully");
        }
    }

    /**
     * Clean up expired tokens (scheduled task)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Cleaning up expired refresh tokens");
        refreshTokenRepository.deleteExpiredAndRevokedTokens();
    }

    /**
     * Map User entity to UserResponse DTO
     */
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
