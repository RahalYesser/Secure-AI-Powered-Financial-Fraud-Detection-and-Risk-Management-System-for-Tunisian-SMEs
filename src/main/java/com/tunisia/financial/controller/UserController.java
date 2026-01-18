package com.tunisia.financial.controller;

import com.tunisia.financial.dto.*;
import com.tunisia.financial.enumerations.UserRole;
import com.tunisia.financial.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user management
 * Provides secure endpoints for user registration, authentication, and account management
 * 
 * Security:
 * - Public endpoints: registration, login
 * - Authenticated endpoints: profile management, password change
 * - Admin endpoints: user management, account locking/unlocking
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user registration, authentication, and account management")
public class UserController {
    
    private final UserService userService;
    
    /**
     * Register a new user
     * Public endpoint
     */
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account. Available roles: ADMIN, FINANCIAL_ANALYST, SME_USER, AUDITOR"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("POST /api/v1/users/register - Registering user: {}", request.email());
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * User login
     * Public endpoint
     */
    @Operation(
        summary = "User login",
        description = "Authenticates a user and returns JWT tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "403", description = "Account locked")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/users/login - Login attempt: {}", request.email());
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current user profile
     * Requires authentication
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestAttribute("userId") UUID userId) {
        log.info("GET /api/v1/users/me - Getting current user: {}", userId);
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get user by ID
     * Admin or self only
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        log.info("GET /api/v1/users/{} - Getting user by ID", userId);
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update user information
     * Admin or self only
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT /api/v1/users/{} - Updating user", userId);
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Change password
     * Authenticated users only (self)
     */
    @PostMapping("/{userId}/change-password")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody PasswordChangeRequest request) {
        log.info("POST /api/v1/users/{}/change-password - Changing password", userId);
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    /**
     * Lock user account
     * Admin only
     */
    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> lockAccount(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int durationMinutes) {
        log.info("POST /api/v1/users/{}/lock - Locking account for {} minutes", userId, durationMinutes);
        userService.lockAccount(userId, durationMinutes);
        return ResponseEntity.ok(Map.of("message", "Account locked successfully"));
    }
    
    /**
     * Unlock user account
     * Admin only
     */
    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> unlockAccount(@PathVariable UUID userId) {
        log.info("POST /api/v1/users/{}/unlock - Unlocking account", userId);
        userService.unlockAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account unlocked successfully"));
    }
    
    /**
     * Delete user account
     * Admin only
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable UUID userId) {
        log.info("DELETE /api/v1/users/{} - Deleting user", userId);
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
    
    /**
     * Get all users with pagination
     * Admin and Auditor only
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        log.info("GET /api/v1/users - Getting all users (page: {}, size: {})", page, size);
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get users by role
     * Admin and Auditor only
     */
    @GetMapping("/by-role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<Page<UserResponse>> getUsersByRole(
            @PathVariable UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/v1/users/by-role/{} - Getting users by role", role);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponse> users = userService.getUsersByRole(role, pageable);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Search users
     * Admin and Auditor only
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/v1/users/search - Searching users: {}", query);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserResponse> users = userService.searchUsers(query, pageable);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user statistics
     * Admin and Auditor only
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<UserStatistics> getUserStatistics() {
        log.info("GET /api/v1/users/statistics - Getting user statistics");
        UserStatistics statistics = userService.getUserStatistics();
        return ResponseEntity.ok(statistics);
    }
}
