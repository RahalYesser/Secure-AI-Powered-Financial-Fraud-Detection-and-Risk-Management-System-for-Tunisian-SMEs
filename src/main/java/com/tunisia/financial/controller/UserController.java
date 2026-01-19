package com.tunisia.financial.controller;

import com.tunisia.financial.dto.*;
import com.tunisia.financial.entity.User;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user management
 * Provides secure endpoints for user administration and account management
 * 
 * Security:
 * - All endpoints require authentication
 * - Admin-only endpoints: user CRUD operations, account locking/unlocking, statistics
 * - Admin/Auditor endpoints: user listing, search, role filtering
 * - Admin or self: view/update user profile
 * 
 * Note: For authentication (login, register), see AuthController
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user administration, CRUD operations, and account management (Admin)")
public class UserController {
    
    private final UserService userService;
    
    /**
     * Get user by ID
     * Admin or self only
     */
    @Operation(
        summary = "Get user by ID",
        description = "Returns user information by ID (Admin or self)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearer-jwt")
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
    @Operation(
        summary = "Update user information",
        description = "Updates user profile information (Admin or self)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearer-jwt")
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
     * Lock user account
     * Admin only
     */
    @Operation(
        summary = "Lock user account",
        description = "Locks a user account for a specified duration (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account locked successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> lockAccount(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int durationMinutes) {
        log.info("POST /api/v1/users/{}/lock - Locking account for {} minutes", userId, durationMinutes);
        userService.lockAccount(userId, durationMinutes);
        return ResponseEntity.ok(Map.of(
            "message", "Account locked successfully",
            "userId", userId.toString(),
            "durationMinutes", String.valueOf(durationMinutes)
        ));
    }
    
    /**
     * Unlock user account
     * Admin only
     */
    @Operation(
        summary = "Unlock user account",
        description = "Unlocks a previously locked user account (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account unlocked successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> unlockAccount(@PathVariable UUID userId) {
        log.info("POST /api/v1/users/{}/unlock - Unlocking account", userId);
        userService.unlockAccount(userId);
        return ResponseEntity.ok(Map.of(
            "message", "Account unlocked successfully",
            "userId", userId.toString()
        ));
    }
    
    /**
     * Delete user account
     * Admin only
     */
    @Operation(
        summary = "Delete user account",
        description = "Permanently deletes a user account (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable UUID userId) {
        log.info("DELETE /api/v1/users/{} - Deleting user", userId);
        userService.deleteUser(userId);
        return ResponseEntity.ok(Map.of(
            "message", "User deleted successfully",
            "userId", userId.toString()
        ));
    }
    
    /**
     * Get all users with pagination
     * Admin and Auditor only
     */
    @Operation(
        summary = "Get all users (paginated)",
        description = "Returns a paginated list of all users (Admin/Auditor only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin or Auditor role required")
    })
    @SecurityRequirement(name = "bearer-jwt")
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
    @Operation(
        summary = "Get users by role",
        description = "Returns a paginated list of users filtered by role (Admin/Auditor only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid role"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin or Auditor role required")
    })
    @SecurityRequirement(name = "bearer-jwt")
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
    @Operation(
        summary = "Search users",
        description = "Searches users by email, first name, or last name (Admin/Auditor only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin or Auditor role required")
    })
    @SecurityRequirement(name = "bearer-jwt")
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
    @Operation(
        summary = "Get user statistics",
        description = "Returns overall user statistics including counts by role (Admin/Auditor only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserStatistics.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin or Auditor role required")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<UserStatistics> getUserStatistics() {
        log.info("GET /api/v1/users/statistics - Getting user statistics");
        UserStatistics statistics = userService.getUserStatistics();
        return ResponseEntity.ok(statistics);
    }
}
