package com.tunisia.financial.service.impl;

import com.tunisia.financial.dto.*;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.UserRole;
import com.tunisia.financial.exception.*;
import com.tunisia.financial.repository.UserRepository;
import com.tunisia.financial.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of UserService interface
 * Handles all user-related business logic with security best practices
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Configuration constants
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;
    
    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new user with email: {}", request.email());
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: User already exists with email: {}", request.email());
            throw new UserAlreadyExistsException(request.email());
        }
        
        // Create new user
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(request.role())
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());
        
        return toUserResponse(savedUser);
    }
    
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.email());
        
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(request.email()));
        
        // Check if account is locked
        if (user.isAccountLocked()) {
            if (user.getLockedUntil() != null && LocalDateTime.now().isAfter(user.getLockedUntil())) {
                // Auto-unlock if lock period expired
                unlockAccount(user.getId());
                user.setAccountLocked(false);
            } else {
                log.warn("Login failed: Account locked for user: {}", request.email());
                throw new AccountLockedException("Account is locked due to multiple failed login attempts", user.getLockedUntil());
            }
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            handleFailedLogin(user);
            log.warn("Login failed: Invalid password for user: {}", request.email());
            throw new InvalidPasswordException("Invalid email or password");
        }
        
        // Reset failed login attempts on successful login
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        
        log.info("Login successful for user: {}", request.email());
        
        return new LoginResponse(
                "token-placeholder", // In production, generate actual JWT token
                "refresh-token-placeholder", // In production, generate actual refresh token
                user.getId(),
                user.getEmail(),
                user.getRole(),
                3600 // Token expires in 1 hour (3600 seconds)
        );
    }
    
    @Override
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return toUserResponse(user);
    }
    
    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return toUserResponse(user);
    }
    
    @Override
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Update fields if provided
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null && !request.email().isBlank() && !request.email().equals(user.getEmail())) {
            // Check if new email already exists
            if (userRepository.existsByEmail(request.email())) {
                throw new UserAlreadyExistsException(request.email());
            }
            user.setEmail(request.email());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);
        
        return toUserResponse(updatedUser);
    }
    
    @Override
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        log.info("Changing password for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            log.warn("Password change failed: Invalid current password for user: {}", userId);
            throw new InvalidPasswordException("Current password is incorrect");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", userId);
    }
    
    @Override
    public void lockAccount(UUID userId, int durationMinutes) {
        log.info("Locking account for user: {} for {} minutes", userId, durationMinutes);
        
        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
        userRepository.lockAccount(userId, lockedUntil);
        
        log.info("Account locked successfully for user: {}", userId);
    }
    
    @Override
    public void unlockAccount(UUID userId) {
        log.info("Unlocking account for user: {}", userId);
        
        userRepository.unlockAccount(userId);
        
        log.info("Account unlocked successfully for user: {}", userId);
    }
    
    @Override
    public void deleteUser(UUID userId) {
        log.info("Deleting user: {}", userId);
        
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        
        userRepository.deleteById(userId);
        
        log.info("User deleted successfully: {}", userId);
    }
    
    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toUserResponse);
    }
    
    @Override
    public Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable)
                .map(this::toUserResponse);
    }
    
    @Override
    public Page<UserResponse> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.searchUsers(searchTerm, pageable)
                .map(this::toUserResponse);
    }
    
    @Override
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long lockedUsers = userRepository.countByAccountLockedTrue();
        long adminCount = userRepository.countByRole(UserRole.ADMIN);
        long analystCount = userRepository.countByRole(UserRole.FINANCIAL_ANALYST);
        long smeUserCount = userRepository.countByRole(UserRole.SME_USER);
        long auditorCount = userRepository.countByRole(UserRole.AUDITOR);
        
        return new UserStatistics(
                totalUsers,
                lockedUsers,
                adminCount,
                analystCount,
                smeUserCount,
                auditorCount
        );
    }
    
    @Override
    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isAccountLocked(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
    
    // ==================== Private Helper Methods ====================
    
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_DURATION_MINUTES));
            log.warn("Account locked due to {} failed login attempts: {}", attempts, user.getEmail());
        }
        
        userRepository.save(user);
    }
    
    private void handleSuccessfulLogin(User user) {
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }
    
    private String generateToken() {
        return UUID.randomUUID().toString();
    }
    
    private String generateAccessToken(User user) {
        // TODO: Implement JWT token generation
        // For now, return a placeholder
        return "access_token_" + user.getId();
    }
    
    private String generateRefreshToken(User user) {
        // TODO: Implement JWT refresh token generation
        // For now, return a placeholder
        return "refresh_token_" + user.getId();
    }
}
