package com.sap.documentssystem.security;

import com.sap.documentssystem.exceptions.JwtAuthenticationException;
import com.sap.documentssystem.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public void validateTokenOrThrow(String token, String expectedUsername) {
        Claims claims = extractAllClaims(token);

        String extractedUsername = claims.getSubject();

        if (!extractedUsername.equals(expectedUsername)) {
            throw new JwtAuthenticationException("JWT username mismatch");
        }

        if (claims.getExpiration().before(new Date())) {
            throw new JwtAuthenticationException("JWT token expired");
        }
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            throw new JwtAuthenticationException("JWT token expired", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtAuthenticationException("Invalid JWT token", ex);
        }
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7 days
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public void validateAccessToken(String token, String expectedUsername) {

        Claims claims = extractAllClaims(token);

        if (!"access".equals(claims.get("type"))) {
            throw new JwtAuthenticationException("Invalid token type");
        }

        validateCommon(claims, expectedUsername);
    }
    public void validateRefreshToken(String token) {

        Claims claims = extractAllClaims(token);

        if (!"refresh".equals(claims.get("type"))) {
            throw new JwtAuthenticationException("Invalid refresh token");
        }
    }
    private void validateCommon(Claims claims, String expectedUsername) {

        if (!claims.getSubject().equals(expectedUsername)) {
            throw new JwtAuthenticationException("JWT username mismatch");
        }

        if (claims.getExpiration().before(new Date())) {
            throw new JwtAuthenticationException("JWT expired");
        }
    }
}