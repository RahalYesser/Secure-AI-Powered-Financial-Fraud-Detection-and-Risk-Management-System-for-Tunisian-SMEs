package com.tunisia.financial.service;

import com.tunisia.financial.dto.*;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for user management operations
 * Defines business logic for user registration, authentication, and account management
 */
public interface UserService {
    
    /**
     * Register a new user
     * Creates user account
     * 
     * @param request User registration data
     * @return Created user response
     * @throws com.tunisia.financial.exception.UserAlreadyExistsException if email already exists
     */
    UserResponse registerUser(UserRegistrationRequest request);
    
    /**
     * Authenticate user with credentials
     * 
     * @param request Login credentials
     * @return Login response with JWT tokens
     * @throws com.tunisia.financial.exception.UserNotFoundException if user not found
     * @throws com.tunisia.financial.exception.InvalidPasswordException if password is incorrect
     * @throws com.tunisia.financial.exception.AccountLockedException if account is locked
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * Get user by ID
     * 
     * @param userId User's unique identifier
     * @return User response
     * @throws com.tunisia.financial.exception.UserNotFoundException if user not found
     */
    UserResponse getUserById(UUID userId);
    
    /**
     * Get user by email
     * 
     * @param email User's email address
     * @return User response
     * @throws com.tunisia.financial.exception.UserNotFoundException if user not found
     */
    UserResponse getUserByEmail(String email);
    
    /**
     * Update user information
     * 
     * @param userId User's ID
     * @param request Update data
     * @return Updated user response
     * @throws com.tunisia.financial.exception.UserNotFoundException if user not found
     */
    UserResponse updateUser(UUID userId, UserUpdateRequest request);
    
    /**
     * Change user password
     * 
     * @param userId User's ID
     * @param request Password change data
     * @throws com.tunisia.financial.exception.UserNotFoundException if user not found
     * @throws com.tunisia.financial.exception.InvalidPasswordException if current password is incorrect
     */
    void changePassword(UUID userId, PasswordChangeRequest request);
    
    /**
     * Lock user account
     * 
     * @param userId User's ID
     * @param durationMinutes Lock duration in minutes
     * @throws com.tunisia.financial.exception.UserNotFoundException if user not found
     */
    void lockAccount(UUID userId, int durationMinutes);
    
    /**
     * Unlock user account
     * 
     * @param userId User's ID
     * @throws com.tunisia.financial.exception.UserNotFoundException if user not found
     */
    void unlockAccount(UUID userId);
    
    /**
     * Delete user account
     * 
     * @param userId User's ID
     * @throws com.tunisia.financial.exception.UserNotFoundException if user not found
     */
    void deleteUser(UUID userId);
    
    /**
     * Get all users with pagination
     * 
     * @param pageable Pagination parameters
     * @return Page of users
     */
    Page<UserResponse> getAllUsers(Pageable pageable);
    
    /**
     * Get users by role
     * 
     * @param role User role
     * @param pageable Pagination parameters
     * @return Page of users with specified role
     */
    Page<UserResponse> getUsersByRole(UserRole role, Pageable pageable);
    
    /**
     * Search users by email or name
     * 
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching users
     */
    Page<UserResponse> searchUsers(String searchTerm, Pageable pageable);
    
    /**
     * Get user statistics
     * 
     * @return User statistics (counts by role, verified, locked, etc.)
     */
    UserStatistics getUserStatistics();
    
    /**
     * Convert User entity to UserResponse DTO
     * 
     * @param user User entity
     * @return User response DTO
     */
    UserResponse toUserResponse(User user);
}
