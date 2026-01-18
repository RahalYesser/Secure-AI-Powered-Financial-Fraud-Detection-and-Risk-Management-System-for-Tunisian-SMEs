package com.tunisia.financial.repository;

import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity
 * Provides database operations for user management
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Find user by email address
     * Used for authentication and user lookup
     * 
     * @param email User's email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if user exists with given email
     * Used for duplicate email validation
     * 
     * @param email Email address to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Find all users by role
     * 
     * @param role User role
     * @param pageable Pagination information
     * @return Page of users with specified role
     */
    Page<User> findByRole(UserRole role, Pageable pageable);
    
    /**
     * Find all locked accounts
     * 
     * @param pageable Pagination information
     * @return Page of locked users
     */
    Page<User> findByAccountLockedTrue(Pageable pageable);
    
    /**
     * Find users who haven't logged in since a specific date
     * Useful for identifying inactive accounts
     * 
     * @param date Date threshold
     * @return List of inactive users
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :date OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsersSince(@Param("date") Instant date);
    
    /**
     * Find users with failed login attempts greater than threshold
     * 
     * @param threshold Failed login attempts threshold
     * @return List of users with excessive failed attempts
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold AND u.accountLocked = false")
    List<User> findUsersWithFailedLoginAttempts(@Param("threshold") int threshold);
    
    /**
     * Find users whose account lock has expired
     * 
     * @param currentTime Current time
     * @return List of users whose lock period has expired
     */
    @Query("SELECT u FROM User u WHERE u.accountLocked = true AND u.lockedUntil IS NOT NULL AND u.lockedUntil < :currentTime")
    List<User> findExpiredLockedAccounts(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Reset failed login attempts for a user
     * 
     * @param userId User's ID
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") UUID userId);
    
    /**
     * Increment failed login attempts
     * 
     * @param userId User's ID
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") UUID userId);
    
    /**
     * Update last login timestamp
     * 
     * @param userId User's ID
     * @param timestamp Login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") UUID userId, @Param("timestamp") Instant timestamp);
    
    /**
     * Lock user account
     * 
     * @param userId User's ID
     * @param lockedUntil Lock expiration time
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = true, u.lockedUntil = :lockedUntil WHERE u.id = :userId")
    void lockAccount(@Param("userId") UUID userId, @Param("lockedUntil") LocalDateTime lockedUntil);
    
    /**
     * Unlock user account
     * 
     * @param userId User's ID
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = false, u.lockedUntil = null, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void unlockAccount(@Param("userId") UUID userId);
    
    /**
     * Count users by role
     * 
     * @param role User role
     * @return Count of users with specified role
     */
    long countByRole(UserRole role);
    
    /**
     * Count locked users
     * 
     * @return Count of locked users
     */
    long countByAccountLockedTrue();
    
    /**
     * Search users by email or name
     * 
     * @param searchTerm Search term
     * @param pageable Pagination information
     * @return Page of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
}
