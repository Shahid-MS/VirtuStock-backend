package com.virtu_stock.Util;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.virtu_stock.Enum.OTPPurpose;
import com.virtu_stock.Models.CustomUserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JWTUtil {

    private final Key jwtKey;

    public JWTUtil(@Value("${jwt.secret.key}") String secret) {
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(CustomUserDetails userDetails) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("name", userDetails.getFullName());
        claims.put("profilePicUrl", userDetails.getUser().getProfilePicUrl());
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList());

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 5))
                .signWith(jwtKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateOTPToken(String email, OTPPurpose purpose) {
        return Jwts.builder()
                .setSubject(email)
                .claim("purpose", purpose)
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .signWith(jwtKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateOTPToken(String token, OTPPurpose expectedPurpose) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String purpose = claims.get("purpose", String.class);

            if (!expectedPurpose.name().equals(purpose)) {
                return null;
            }

            return email;

        } catch (ExpiredJwtException ex) {
            return null;
        } catch (JwtException ex) {
            return null;
        }
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        List<?> rawList = claims.get("roles", List.class);
        return rawList.stream().map(Object::toString).toList();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
}
