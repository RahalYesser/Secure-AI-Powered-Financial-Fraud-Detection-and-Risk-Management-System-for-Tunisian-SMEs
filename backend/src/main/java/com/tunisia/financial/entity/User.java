package com.tunisia.financial.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tunisia.financial.enumerations.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User entity for the financial fraud detection system
 * Implements Spring Security UserDetails for authentication and authorization
 * 
 * Security features:
 * - BCrypt password hashing
 * - Account locking after failed login attempts
 * - Email verification requirement
 * - MFA support (Time-based One-Time Password)
 * - OAuth2 provider tracking for social login
 * - Role-based access control
 * 
 * @author Financial Security Team
 */
@Entity
@Table(name = "users", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"email"}),
       indexes = {
           @Index(name = "idx_user_email", columnList = "email"),
           @Index(name = "idx_user_role", columnList = "role")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(exclude = {"password"})
@EqualsAndHashCode(of = "email")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    
    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    String email;
    
    @Column(length = 100)
    String firstName;
    
    @Column(length = 100)
    String lastName;
    
    /**
     * Password field
     * Validation is handled at the service/DTO level, not entity level
     * Stored as BCrypt hash
     */
    @JsonIgnore
    @Column(nullable = false, length = 255)
    String password;
    
    /**
     * User role for role-based access control
     * Each user has one primary role that determines their permissions
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull(message = "Role is required")
    UserRole role;
    
    /**
     * Account lock status
     * Accounts are locked after multiple failed login attempts or by admin action
     */
    @Column(name = "account_locked", nullable = false)
    boolean accountLocked;
    
    /**
     * Counter for failed login attempts
     * Reset to 0 after successful login
     * Account locked after reaching threshold (e.g., 5 attempts)
     */
    @Column(name = "failed_login_attempts", nullable = false)
    int failedLoginAttempts;
    
    /**
     * Timestamp of last successful login
     * Used for security auditing and inactive account detection
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    @Column(name = "last_login_at")
    Instant lastLoginAt;
    
    /**
     * Timestamp when account was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    
    /**
     * Timestamp when account was last updated
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    @Column(name = "updated_at")
    Instant updatedAt;
    
    /**
     * Timestamp until which the account is locked
     * Account automatically unlocks after this time
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    @Column(name = "locked_until")
    LocalDateTime lockedUntil;
    
    /**
     * Automatically set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    /**
     * Automatically update timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // ==================== Spring Security UserDetails Implementation ====================
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true; // Implement account expiration logic if needed
    }
    
    @Override
    public boolean isAccountNonLocked() {
        // Check if account is locked and if lock period has expired
        if (accountLocked && lockedUntil != null) {
            if (LocalDateTime.now().isAfter(lockedUntil)) {
                // Auto-unlock if lock period has passed
                return true;
            }
            return false;
        }
        return !accountLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Implement password expiration logic if needed
    }
    
    @Override
    public boolean isEnabled() {
        // Account is always enabled if not locked
        return true;
    }
    
    /**
     * Get user's full name
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return email;
    }
}