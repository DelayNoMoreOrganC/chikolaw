package com.lawfirm.security;

import com.lawfirm.entity.User;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 安全工具类 - 获取当前用户信息
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * 从 Spring Security Context 中获取当前用户ID
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidParameterException("authentication", "用户未登录");
        }

        // JwtAuthenticationFilter 将 userId (Long类型) 设置为 principal
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        } else if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                // 如果是字符串格式的用户名，查询数据库
                return userRepository.findByUsername((String) principal)
                        .map(User::getId)
                        .orElseThrow(() -> new InvalidParameterException("principal", "无效的用户信息"));
            }
        } else if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            String username = userDetails.getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                return userRepository.findByUsername(username)
                        .map(User::getId)
                        .orElseThrow(() -> new InvalidParameterException("principal", "无效的用户信息"));
            }
        }

        throw new InvalidParameterException("principal", "无法识别的用户信息类型: " + principal.getClass());
    }

    /**
     * 获取当前用户的角色编码列表
     */
    public Set<String> getCurrentUserRoles() {
        Long userId = getCurrentUserId();
        return userRoleRepository.findByUserId(userId)
                .stream()
                .map(userRole -> userRole.getRole().getRoleCode())
                .collect(Collectors.toSet());
    }

    /**
     * 判断当前用户是否为管理员
     */
    public boolean isAdmin() {
        Set<String> roles = getCurrentUserRoles();
        // 兼容 roleCode 和 roleName
        return roles.contains("ADMIN") || roles.contains("MANAGER") || roles.contains("DIRECTOR")
            || roles.contains("管理员") || roles.contains("主任");
    }

    /**
     * 判断当前用户是否有指定角色
     */
    public boolean hasRole(String roleCode) {
        return getCurrentUserRoles().contains(roleCode);
    }
}
