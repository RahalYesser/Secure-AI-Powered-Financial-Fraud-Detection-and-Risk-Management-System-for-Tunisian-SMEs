package com.tunisia.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

/**
 * Validation error response DTO
 * Used for field validation errors
 * 
 * @param status HTTP status code
 * @param message Error message
 * @param errors Map of field names to error messages
 * @param timestamp Error timestamp
 */
public record ValidationErrorResponse(
    int status,
    String message,
    Map<String, String> errors,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    Instant timestamp
) {}
