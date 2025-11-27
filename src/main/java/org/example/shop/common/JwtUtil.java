// src/main/java/org/example/shop/common/JwtUtil.java
package org.example.shop.common;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // 密钥必须 ≥ 512 bit（64 字节），HS512 要求
    private final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            "shop-mini-secret-key-2025-must-be-long-enough-for-hs512-1234567890".getBytes(StandardCharsets.UTF_8)
    );

    // 7 天有效期
    private final long EXPIRATION = 1000L * 60 * 60 * 24 * 7;

    /** 生成 Token */
    public String generateToken(Long userId, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))   // subject 存 userId
                .claim("role", role)                  // 自定义 claim 存角色
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SECRET_KEY)                 // HS512 签名
                .compact();
    }

    /** 安全解析（任何异常都返回 null，不会抛出） */
    private Claims safeParse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException |
                 SecurityException | IllegalArgumentException e) {
            return null;   // 所有异常统统吃掉
        }
    }

    /** 安全获取 userId */
    public Long getUserId(String token) {
        try {
            Claims claims = safeParse(token);
            if (claims == null) return null;
            String sub = claims.getSubject();
            return sub == null ? null : Long.valueOf(sub);
        } catch (Exception e) {
            return null;
        }
    }

    /** 安全获取 role */
    public String getRole(String token) {
        try {
            Claims claims = safeParse(token);
            return claims == null ? null : claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** 验证 token 是否有效 */
    public boolean validateToken(String token) {
        return safeParse(token) != null;
    }
}