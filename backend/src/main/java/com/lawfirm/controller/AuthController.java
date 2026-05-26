package com.lawfirm.controller;

import com.lawfirm.entity.User;
import com.lawfirm.exception.AuthenticationFailedException;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.util.JwtUtil;
import com.lawfirm.util.RedisUtil;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final com.lawfirm.security.LoginAttemptCache loginAttemptCache;

    /** 是否启用登录失败锁定（开发环境可在 application-dev 中关闭） */
    @Value("${login.lock-enabled:true}")
    private boolean loginLockEnabled;

    @Autowired(required = false)
    private RedisUtil redisUtil;

    /**
     * 登录请求DTO
     */
    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    /**
     * 修改密码请求DTO
     * 安全要求：密码长度≥8位，包含大小写字母、数字
     */
    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "旧密码不能为空")
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        @com.lawfirm.validation.PasswordStrength(
            message = "新密码必须至少8位，包含大小写字母和数字"
        )
        private String newPassword;
    }

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 检查是否仍在锁定期（必须用 isLocked，不能仅用次数≥5，否则锁定期结束后在清理任务运行前会一直被拒）
        if (loginLockEnabled && loginAttemptCache.isLocked(username)) {
            long remainingMinutes = loginAttemptCache.getRemainingLockTime(username);
            throw new AuthenticationFailedException(
                String.format("登录失败次数过多，账号已被锁定，请%d分钟后再试", Math.max(1, remainingMinutes))
            );
        }

        try {
            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // 获取用户信息
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("用户", "username", username));

            // 检查用户状态
            if (user.getStatus() == 0) {
                throw new AuthenticationFailedException("账号已被禁用，请联系管理员");
            }

            // 生成Token（包含权限信息）
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), authentication.getAuthorities());

            // 清除登录失败次数
            loginAttemptCache.clearFailedAttempts(username);

            // 更新最后登录时间
            user.setLastLoginTime(java.time.LocalDateTime.now());
            userRepository.save(user);

            // 构造返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("email", user.getEmail());
            data.put("phone", user.getPhone());
            data.put("avatar", user.getAvatar());

            log.info("用户登录成功: {}", username);
            return Result.success(data);

        } catch (Exception e) {
            if (loginLockEnabled) {
                loginAttemptCache.recordFailedAttempt(username);
                int currentFailCount = loginAttemptCache.getFailedAttempts(username);
                log.warn("用户登录失败: {}, 失败次数: {}", username, currentFailCount);
                if (currentFailCount >= 5) {
                    throw new AuthenticationFailedException(
                        "登录失败次数过多，账号已被锁定30分钟"
                    );
                }
            } else {
                log.warn("用户登录失败: {} (开发环境未启用登录锁定)", username);
            }

            throw new AuthenticationFailedException("用户名或密码错误");
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current-user")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            throw new InvalidParameterException("Authorization", "认证令牌不能为空");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new InvalidParameterException("Authorization", "无效的认证令牌格式");
        }
        String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
        Long userId = jwtUtil.getUserIdFromToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("avatar", user.getAvatar());
        data.put("departmentId", user.getDepartmentId());
        data.put("position", user.getPosition());

        return Result.success(data);
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> changePassword(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        if (authHeader == null || authHeader.isEmpty()) {
            throw new InvalidParameterException("Authorization", "认证令牌不能为空");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new InvalidParameterException("Authorization", "无效的认证令牌格式");
        }
        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new InvalidParameterException("oldPassword", "旧密码错误");
        }

        // 设置新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("用户修改密码成功: {}", user.getUsername());
        return Result.success();
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT是无状态的，前端只需删除Token即可
        // 如果需要Token黑名单，可以在这里实现
        log.info("用户登出成功");
        return Result.success();
    }
}
