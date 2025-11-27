// src/main/java/com/shop/controller/AuthController.java
package org.example.shop.controller;

import org.example.shop.annotation.CurrentUser;
import org.example.shop.common.JwtUtil;
import org.example.shop.common.Result;
import org.example.shop.entity.User;
import org.example.shop.service.impl.UserServiceImpl;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserServiceImpl userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ──────────────────
    // 注册（无需验证码）
    // ──────────────────
    @Data
    static class RegisterDTO {
        private String phone;     // 手机号
        private String password;  // 密码（明文传输，后端加密）
        private String role;      // 必须传 "user" 或 "merchant"
    }

    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO dto) {
        // 1. 参数校验
        if (dto.getPhone() == null || dto.getPassword() == null || dto.getRole() == null) {
            return Result.fail("参数不完整");
        }
        if (!dto.getRole().equals("user") && !dto.getRole().equals("merchant")) {
            return Result.fail("角色只能是 user 或 merchant");
        }
        if (userService.lambdaQuery().eq(User::getPhone, dto.getPhone()).exists()) {
            return Result.fail("该手机号已注册");
        }

        // 2. 创建用户
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname("用户" + dto.getPhone().substring(7));
        user.setRole(dto.getRole());
        user.setCreateTime(LocalDateTime.now());
        userService.save(user);

        // 3. 直接返回 token（注册即登录）
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        return Result.ok(Map.of(
                "token", token,
                "userId", user.getId(),
                "phone", user.getPhone(),
                "role", user.getRole(),
                "nickname", user.getNickname()
        ));
    }

    // ──────────────────
    // 登录
    // ──────────────────
    @Data
    static class LoginDTO {
        private String phone;
        private String password;
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO dto) {
        User user = userService.lambdaQuery()
                .eq(User::getPhone, dto.getPhone())
                .one();

        if (user != null && passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            String token = jwtUtil.generateToken(user.getId(), user.getRole());
            return Result.ok(Map.of(
                    "token", token,
                    "userId", user.getId(),
                    "role", user.getRole(),
                    "nickname", user.getNickname()
            ));
        }
        return Result.fail("手机号或密码错误");
    }

    // 在 AuthController 加这一个接口
    @GetMapping("/auth/info")
    public Result info(@CurrentUser User user) {
        return Result.ok(user);
    }
}