package com.tunisia.financial.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Utility Class
 * 
 * This class handles JWT (JSON Web Token) creation, validation, and extraction.
 * 
 * What is JWT?
 * - JWT is a compact, URL-safe token format for securely transmitting information
 * - It consists of three parts: Header.Payload.Signature
 * - The signature ensures the token hasn't been tampered with
 * 
 * How it works in this application:
 * 1. User logs in with email/password
 * 2. This utility generates a JWT token containing user information (email, role)
 * 3. Token is sent to the client (frontend)
 * 4. Client includes token in Authorization header for subsequent requests
 * 5. JwtAuthenticationFilter validates the token and authenticates the user
 * 
 * Token Structure:
 * - Header: Algorithm and token type (HS256, JWT)
 * - Payload: Claims (user email, role, expiration time)
 * - Signature: HMAC SHA256 signature using secret key
 * 
 * Security Notes:
 * - Secret key must be kept secure (stored in application.properties)
 * - Tokens expire after a set time (24 hours for access token, 7 days for refresh token)
 * - Tokens are signed, so any modification invalidates them
 */
@Component
public class JwtUtil {

    // Secret key for signing JWT tokens
    // This should be a strong, random string (at least 256 bits)
    // In production, use environment variables or a secrets manager
    @Value("${jwt.secret}")
    private String secret;

    // Access token expiration time in milliseconds
    // Default: 86400000 ms = 24 hours
    @Value("${jwt.expiration}")
    private Long expiration;

    // Refresh token expiration time in milliseconds
    // Default: 604800000 ms = 7 days
    @Value("${jwt.refresh.expiration}")
    private Long refreshExpiration;

    /**
     * Get the signing key for JWT tokens
     * 
     * This converts the secret string into a SecretKey object
     * that can be used to sign and verify JWT tokens.
     * 
     * @return SecretKey for HMAC SHA256 signing
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extract username (email) from JWT token
     * 
     * The username is stored in the "sub" (subject) claim of the JWT.
     * 
     * @param token JWT token string
     * @return Username (email) from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from JWT token
     * 
     * @param token JWT token string
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from JWT token
     * 
     * This is a generic method that can extract any claim from the token.
     * 
     * @param token JWT token string
     * @param claimsResolver Function to extract the desired claim
     * @return The extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from JWT token
     * 
     * This parses the JWT token and validates its signature.
     * If the signature is invalid, an exception is thrown.
     * 
     * @param token JWT token string
     * @return All claims from the token
     */
    private Claims extractAllClaims(String token) {

        //ensure that the token is not blank or full of whitespace before parsing it
        if(token == null || token.isBlank()) {
            throw new IllegalArgumentException("Jwt Token cannot be blank or full of whitespace");
        }

        // Compatible with JJWT 0.12.x API
        // This validates the token signature and extracts the payload
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }

    /**
     * Check if JWT token is expired
     * 
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generate access token for a user
     * 
     * Access tokens are short-lived (24 hours) and contain user information.
     * They are used for authenticating API requests.
     * 
     * @param userDetails User details (User entity implements UserDetails)
     * @return JWT access token
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Extract role from user's authorities and add to token
        // This allows role-based access control
        claims.put("role", userDetails.getAuthorities().stream().findFirst().orElseThrow().getAuthority());
        return createToken(claims, userDetails.getUsername(), expiration);
    }

    /**
     * Generate refresh token for a user
     * 
     * Refresh tokens are long-lived (7 days) and are used to obtain new access tokens
     * without requiring the user to login again.
     * 
     * @param userDetails User details (User entity implements UserDetails)
     * @return JWT refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Refresh tokens don't need role information
        // They're only used to generate new access tokens
        return createToken(claims, userDetails.getUsername(), refreshExpiration);
    }

    /**
     * Create a JWT token with the specified claims
     * 
     * This is the core method that builds the JWT token.
     * 
     * @param claims Custom claims to include in the token (e.g., role)
     * @param subject Subject claim (usually the username/email)
     * @param expirationTime Token expiration time in milliseconds
     * @return Signed JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        return Jwts.builder()
                .setClaims(claims) // Custom claims (role, etc.)
                .setSubject(subject) // User identifier (email)
                .setIssuedAt(new Date(System.currentTimeMillis())) // Token creation time
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // Token expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Sign with HMAC SHA256
                .compact(); // Build and return as compact string
    }

    /**
     * Validate JWT token
     * 
     * Checks if:
     * 1. Token signature is valid (not tampered with)
     * 2. Token is not expired
     * 3. Token belongs to the specified user
     * 
     * @param token JWT token string
     * @param userDetails User details to validate against
     * @return true if token is valid, false otherwise
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            // Check if token username matches user and token is not expired
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            // If token parsing fails, it's invalid
            return false;
        }
    }

    /**
     * Extract role from JWT token
     * 
     * The role is stored in the "role" claim of the token.
     * 
     * @param token JWT token string
     * @return Role string (e.g., "ROLE_ADMIN", "ROLE_USER")
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
}
