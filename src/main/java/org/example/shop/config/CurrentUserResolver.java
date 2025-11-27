// src/main/java/org/example/shop/config/CurrentUserResolver.java
package org.example.shop.config;

import lombok.RequiredArgsConstructor;
import org.example.shop.annotation.CurrentUser;
import org.example.shop.common.JwtUtil;
import org.example.shop.entity.User;
import org.example.shop.service.impl.UserServiceImpl;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    private final UserServiceImpl userService;
    private final JwtUtil jwtUtil;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        final String authHeader = webRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        final String token = authHeader.substring(7);

        // 第一步：先从 token 里直接拿 userId 和 role（最保险！）
        Long userId = jwtUtil.getUserId(token);
        String roleFromToken = jwtUtil.getRole(token);

        System.out.println("=== JWT 直接解析结果 ===");
        System.out.println("userId: " + userId);
        System.out.println("roleFromToken: " + roleFromToken);
        System.out.println("=============================");

        if (userId == null) {
            return null;   // token 无效或过期
        }

        // 第二步：从数据库查完整用户
        User user = userService.lambdaQuery()
                .eq(User::getId, userId)
                .one();

        // 第三步：如果数据库 role 是 null，就用 token 里的补上（永不为 null！）
        if (user != null && user.getRole() == null && roleFromToken != null) {
            user.setRole(roleFromToken);
        }

        // 最后打印最终注入的对象
        System.out.println("=== 最终注入的 User 对象 ===");
        System.out.println("userId: " + (user != null ? user.getId() : "null"));
        System.out.println("role: " + (user != null ? user.getRole() : "null"));
        System.out.println("================================");

        return user;
    }
}