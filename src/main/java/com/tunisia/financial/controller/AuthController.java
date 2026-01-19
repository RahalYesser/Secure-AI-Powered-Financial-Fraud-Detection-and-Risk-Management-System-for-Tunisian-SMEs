package com.tunisia.financial.controller;

import com.tunisia.financial.dto.*;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for authentication and authorization
 * Provides secure endpoints for user registration, login, and password management
 * 
 * Security:
 * - Public endpoints: registration, login
 * - Authenticated endpoints: current user profile, password change
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "APIs for user authentication, registration, and password management")
public class AuthController {
    
    private final UserService userService;
    
    /**
     * Register a new user
     * Public endpoint - No authentication required
     */
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account. Available roles: ADMIN, FINANCIAL_ANALYST, SME_USER, AUDITOR"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "User already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("POST /api/v1/auth/register - Registering user: {}", request.email());
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * User login
     * Public endpoint - No authentication required
     */
    @Operation(
        summary = "User login",
        description = "Authenticates a user and returns JWT access and refresh tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Account locked",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/auth/login - Login attempt: {}", request.email());
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current authenticated user profile
     * Requires JWT authentication
     */
    @Operation(
        summary = "Get current user profile",
        description = "Returns the profile of the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        log.info("GET /api/v1/auth/me - Getting current user: {}", user.getEmail());
        UserResponse response = userService.getUserById(user.getId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Change password for current authenticated user
     * Requires JWT authentication
     */
    @Operation(
        summary = "Change password",
        description = "Allows authenticated users to change their own password"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid password format or old password incorrect",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PasswordChangeRequest request) {
        log.info("POST /api/v1/auth/change-password - Changing password for user: {}", user.getEmail());
        userService.changePassword(user.getId(), request);
        return ResponseEntity.ok(Map.of(
            "message", "Password changed successfully",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
    
    /**
     * Logout (placeholder for token invalidation)
     * In a stateless JWT system, logout is typically handled client-side by removing the token
     * This endpoint can be used for logging/auditing purposes
     */
    @Operation(
        summary = "User logout",
        description = "Logs out the current user (client should discard JWT tokens)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal User user) {
        log.info("POST /api/v1/auth/logout - User logging out: {}", user.getEmail());
        return ResponseEntity.ok(Map.of(
            "message", "Logout successful. Please discard your JWT tokens.",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
