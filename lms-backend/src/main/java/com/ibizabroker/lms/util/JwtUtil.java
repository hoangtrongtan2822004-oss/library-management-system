// src/main/java/com/ibizabroker/lms/util/JwtUtil.java
package com.ibizabroker.lms.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.io.DecodingException;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

  @Value("${app.jwt-secret:dev-secret-change-me-32-bytes-minimum!!}")
  private String secret; // >= 32 bytes (hoặc base64)

  @Value("${app.jwt-expiration:86400000}")
  private long expirationMs;

  @Value("${app.jwt-refresh-expiration:604800000}") // 7 days default
  private long refreshExpirationMs;

  /**
   * 🔑 Generate SecretKey from Base64-encoded secret
   * 
   * ⚠️ SECURITY: Bắt buộc dùng Base64 - Không fallback sang plain text
   * - Nếu decode lỗi → Throw exception để Admin biết config sai
   * - Plain text fallback = security risk (weak key)
   * 
   * ✅ Generate Base64 secret: 
   * ```bash
   * openssl rand -base64 32
   * ```
   * 
   * @throws IllegalArgumentException if secret is not valid Base64
   */
  private SecretKey key() {
    try {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes). Current: " + keyBytes.length + " bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    } catch (DecodingException e) {
        throw new IllegalArgumentException(
            "JWT secret must be Base64-encoded. Generate with: openssl rand -base64 32. Error: " + e.getMessage(), e
        );
    }
}

private Date currentTime = null;

public void setCurrentTime(Date currentTime) {
    this.currentTime = currentTime;
}

private Date now() {
    return currentTime != null ? currentTime : new Date();
}

// ⭐ BẮT ĐẦU THÊM TỪ ĐÂY
  /**
   * Dùng cho mục đích Test (Unit Test)
   */
  public void setSecret(String secret) {
      this.secret = secret;
  }

  /**
   * Dùng cho mục đích Test (Unit Test)
   */
  public void setExpirationMs(long expirationMs) {
      this.expirationMs = expirationMs;
  }
  // ⭐ KẾT THÚC THÊM Ở ĐÂY

  /** ✅ Dùng hàm này: thêm extra claims (userId, roles, ...) */
  public String generateToken(UserDetails user, Map<String, Object> extra) {
    Date now = now();
    Date exp = new Date(now.getTime() + expirationMs);
    Map<String, Object> claims = (extra == null) ? new HashMap<>() : new HashMap<>(extra);
    return Jwts.builder()
        .claims(claims)
        .subject(user.getUsername())
        .issuedAt(now)
        .expiration(exp)
        .signWith(key())
        .compact();
  }

  /** ✅ Generate refresh token with longer expiry (7 days) */
  public String generateRefreshToken(UserDetails user, Map<String, Object> extra) {
    Date now = now();
    Date exp = new Date(now.getTime() + refreshExpirationMs);
    Map<String, Object> claims = (extra == null) ? new HashMap<>() : new HashMap<>(extra);
    claims.put("tokenType", "REFRESH"); // Mark as refresh token
    return Jwts.builder()
        .claims(claims)
        .subject(user.getUsername())
        .issuedAt(now)
        .expiration(exp)
        .signWith(key())
        .compact();
  }

  public String getUsernameFromToken(String token) {
    return getClaim(token, Claims::getSubject);
  }

  /**
   * ✅ Validate JWT Token (Enterprise Security)
   * 
   * Security Checks:
   * 1. Signature verification (automatic via parseAllClaims)
   * 2. Token not expired
   * 3. Username matches UserDetails
   * 4. **NEW**: Reject Refresh Token for API access (tokenType check)
   * 
   * ⚠️ IMPORTANT: Refresh tokens can ONLY be used at /api/auth/refresh
   * Using refresh token for API calls = security violation
   * 
   * 🔮 TODO (Enterprise Feature): Token Blacklisting
   * Add Redis-based token revocation for logout:
   * ```java
   * // Check if token is blacklisted (logged out)
   * String jti = claims.get("jti", String.class); // JWT ID
   * if (redisTemplate.hasKey("blacklist:" + jti)) {
   *     return false; // Token revoked
   * }
   * ```
   * Implementation requires:
   * - Add "jti" claim in generateToken()
   * - RedisTemplate<String, String> injection
   * - Logout endpoint: PUT token jti in Redis with TTL = token remaining time
   * 
   * @param token JWT token string
   * @param user Expected user (from database)
   * @return true if token is valid for API access
   */
  public boolean validateToken(String token, UserDetails user) {
    try {
      Claims claims = parseAllClaims(token);
      
      // 🔒 SECURITY: Block refresh token from being used as access token
      Object tokenType = claims.get("tokenType");
      if ("REFRESH".equals(tokenType)) {
        return false; // Refresh tokens can only be used at /auth/refresh endpoint
      }
      
      String username = claims.getSubject();
      return username != null && username.equals(user.getUsername()) && !isExpired(token);
    } catch (ExpiredJwtException e) {
      return false;
    } catch (Exception e) {
      // Invalid signature, malformed token, etc.
      return false;
    }
  }

  private boolean isExpired(String token) {
    Date exp = getClaim(token, Claims::getExpiration);
    return exp.before(now());
  }

  private <T> T getClaim(String token, Function<Claims, T> resolver) {
    return resolver.apply(parseAllClaims(token));
  }

  public Claims parseAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(key())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /** ❌ Không dùng nữa */
  @Deprecated
  public String generateToken(String username) {
    throw new UnsupportedOperationException("Use generateToken(UserDetails, extraClaims)");
  }

  /**
   * ⚠️ DEPRECATED: Extract userId from Authentication object
   * 
   * 🔄 Migration Path:
   * Use SecurityUtils.getCurrentUserId() instead for cleaner code:
   * 
   * ```java
   * // OLD (BAD):
   * Integer userId = jwtUtil.extractUserIdFromAuth(authentication);
   * 
   * // NEW (GOOD):
   * Integer userId = SecurityUtils.getCurrentUserId()
   *     .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
   * ```
   * 
   * Or better: Use UsersRepository directly
   * ```java
   * Users user = usersRepository.findByUsername(SecurityUtils.getCurrentUsername().orElseThrow())
   *     .orElseThrow(() -> new NotFoundException("User not found"));
   * Integer userId = user.getUserId();
   * ```
   * 
   * @deprecated Since Phase 8, use {@link com.ibizabroker.lms.util.SecurityUtils#getCurrentUserId()} instead
   */
  @Deprecated(since = "Phase 8", forRemoval = true)
  public Integer extractUserIdFromAuth(org.springframework.security.core.Authentication auth) {
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("Authentication is required");
    }
    
    // Get from authentication details/credentials if stored as JWT
    Object credentials = auth.getCredentials();
    if (credentials instanceof String token) {
      try {
        Claims claims = parseAllClaims(token);
        Object userId = claims.get("userId");
        if (userId != null) {
          return userId instanceof Integer ? (Integer) userId : Integer.parseInt(userId.toString());
        }
      } catch (Exception ignored) {}
    }
    
    // Fallback: get userId from name if it's numeric
    String name = auth.getName();
    if (name != null && name.matches("\\d+")) {
      return Integer.parseInt(name);
    }
    
    throw new IllegalStateException("Cannot extract userId from authentication - use SecurityUtils.getCurrentUserId() or UsersRepository.findByUsername instead");
  }
}
