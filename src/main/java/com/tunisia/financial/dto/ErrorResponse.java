package com.tunisia.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Standard error response DTO
 * 
 * @param status HTTP status code
 * @param error Error type
 * @param message Error message
 * @param timestamp Error timestamp
 */
public record ErrorResponse(
    int status,
    String error,
    String message,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    Instant timestamp
) {}
