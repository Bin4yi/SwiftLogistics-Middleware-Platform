// services/driver-service/src/main/java/com/swiftlogistics/driver/service/JwtTokenService.java
package com.swiftlogistics.driver.service;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    // Make sure the secret is at least 256 bits (32 characters) for HS256
    @Value("${app.jwt.secret:swiftlogistics-driver-secret-key-2024-very-long-secure-key}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpiration;

    private String getSigningKey() {
        // Ensure consistent encoding and proper key length
        String secret = jwtSecret;
        if (secret.length() < 32) {
            logger.warn("JWT secret key is too short. Padding to minimum length.");
            // Pad the key if it's too short (not recommended for production)
            StringBuilder paddedSecret = new StringBuilder(secret);
            while (paddedSecret.length() < 32) {
                paddedSecret.append("0");
            }
            secret = paddedSecret.toString();
        }
        return secret;
    }

    public String generateToken(String driverId) {
        if (driverId == null || driverId.trim().isEmpty()) {
            throw new IllegalArgumentException("Driver ID cannot be null or empty");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(driverId.trim())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("swift-logistics-driver-service")
                .signWith(SignatureAlgorithm.HS256, getSigningKey())
                .compact();
    }

    public String getDriverIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token.trim())
                    .getBody();

            return claims.getSubject();
        } catch (JwtException e) {
            logger.error("Failed to extract driver ID from token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.debug("Token is null or empty");
            return false;
        }

        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token.trim());
            return true;
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error validating token: {}", e.getMessage());
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        if (token == null || token.trim().isEmpty()) {
            return true;
        }

        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token.trim())
                    .getBody();

            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            logger.debug("Token is expired: {}", e.getMessage());
            return true;
        } catch (JwtException e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .parseClaimsJws(token.trim())
                    .getBody();

            return claims.getExpiration();
        } catch (JwtException e) {
            logger.error("Failed to extract expiration date from token: {}", e.getMessage());
            return null;
        }
    }

    public long getTimeUntilExpiration(String token) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return 0;
        }
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }
}