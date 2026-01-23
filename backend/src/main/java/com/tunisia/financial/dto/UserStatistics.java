package com.tunisia.financial.dto;

/**
 * User statistics DTO
 * Contains aggregated user data for dashboard
 * 
 * @param totalUsers Total number of users
 * @param lockedUsers Number of locked users
 * @param adminCount Number of admin users
 * @param analystCount Number of financial analyst users
 * @param smeUserCount Number of SME users
 * @param auditorCount Number of auditor users
 */
public record UserStatistics(
    long totalUsers,
    long lockedUsers,
    long adminCount,
    long analystCount,
    long smeUserCount,
    long auditorCount
) {}
