// src/main/java/org/example/shop/utils/JwtAuthenticationFilter.java
package org.example.shop.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.shop.common.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            // 使用新版安全方法
            Long userId = jwtUtil.getUserId(token);
            String role = jwtUtil.getRole(token);

            if (userId != null && role != null) {
                // 构造一个简单的 Authentication 对象放进 Security 上下文
                var auth = new UsernamePasswordAuthenticationToken(
                        userId.toString(),      // principal
                        null,                   // credentials
                        java.util.List.of(() -> "ROLE_" + role.toUpperCase())  // 权限
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}