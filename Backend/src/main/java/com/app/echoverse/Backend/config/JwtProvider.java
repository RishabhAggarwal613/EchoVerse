package com.app.echoverse.Backend.config;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtProvider {

    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 1 day in ms

    private final SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());

    // Generates a JWT token with email and authorities
    public String generateToken(Authentication auth) {
        String authorities = populateAuthorities(auth.getAuthorities());

        return Jwts.builder()
                .setSubject(auth.getName()) // better to use subject for identity
                .claim("email", auth.getName())
                .claim("authorities", authorities)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    // Extracts email from JWT token
    public String getEmailFromJwtToken(String jwt) {
        jwt = jwt.substring(7); 
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt)
                .getBody();

        return claims.get("email", String.class);
    }

    // Convert authorities to a comma-separated string
    public String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<String> auths = new HashSet<>();
        for (GrantedAuthority authority : authorities) {
            auths.add(authority.getAuthority());
        }
        return String.join(",", auths);
    }
}
