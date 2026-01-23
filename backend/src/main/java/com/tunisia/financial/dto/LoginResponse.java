package com.tunisia.financial.dto;

import com.tunisia.financial.enumerations.UserRole;

import java.util.UUID;

/**
 * Login response DTO
 * Returned after successful authentication
 * 
 * @param token JWT access token
 * @param refreshToken JWT refresh token
 * @param userId User's ID
 * @param email User's email
 * @param role User's role
 * @param expiresIn Token expiration time in seconds
 */
public record LoginResponse(
    String token,
    String refreshToken,
    UUID userId,
    String email,
    UserRole role,
    long expiresIn
) {}
