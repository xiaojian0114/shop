// config/SecurityConfig.java （终极无敌版，2025年最新写法）
package org.example.shop.config;

import lombok.RequiredArgsConstructor;
import org.example.shop.common.JwtUtil;
import org.example.shop.utils.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. 完全公开：登录注册
                        .requestMatchers("/auth/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/merchant/upload").authenticated()    // 上传要登录
                        .requestMatchers("/upload/**").permitAll()                               // 图片访问完全公开
                        .requestMatchers("/merchant/upload/**").permitAll()

                        // 2. 只要登录就能访问（买家、商家都行！）
                        .requestMatchers(
                                "/user/products",
                                "/user/product/**",
                                "/user/shop/info/**",      // 重点放行这个！
                                "/user/cart",
                                "/user/cart/**",
                                "/user/order/**"

                        ).authenticated()  // 只要有token就行，不校验角色！

                        // 3. 商家专属接口（必须是商家角色）
                        .requestMatchers("/merchant/**").hasRole("MERCHANT")

                        // 4. 管理员专属接口（必须是管理员角色）
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 5. 其他所有接口都要登录
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}