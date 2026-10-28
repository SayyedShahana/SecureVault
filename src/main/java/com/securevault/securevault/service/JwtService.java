package com.securevault.securevault.service;

import com.securevault.securevault.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey signingKey;
  private final long expirationMs;

  public JwtService(
      @Value("${securevault.jwt.secret}") String secret,
      @Value("${securevault.jwt.access-expiration-ms:900000}") long expirationMs) {

    byte[] keyBytes = Decoders.BASE64.decode(secret);

    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    this.expirationMs = expirationMs;
  }

  public String generateAccessToken(User user) {
    Instant now = Instant.now();

    return Jwts.builder()
        .subject(user.getEmail())
        .claim("role", user.getRole().getRoleName())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(expirationMs)))
        .signWith(signingKey)
        .compact();
  }

  public Claims extractClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  public boolean isTokenValid(String token, String email) {
    try {
      Claims claims = extractClaims(token);

      return email.equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
      return false;
    }
  }
}
