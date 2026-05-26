package com.lawfirm.service;

import com.lawfirm.dto.UserCreateRequest;
import com.lawfirm.dto.UserDTO;
import com.lawfirm.dto.UserUpdateRequest;
import com.lawfirm.entity.User;
import com.lawfirm.entity.UserRole;
import com.lawfirm.enums.UserStatus;
import com.lawfirm.exception.DuplicateResourceException;
import com.lawfirm.exception.InvalidParameterException;
import com.lawfirm.exception.ResourceNotFoundException;
import com.lawfirm.repository.DepartmentRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 * 支持缓存：用户查询结果会被缓存，减少数据库查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建用户
     * 创建时清除users缓存
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", allEntries = true)
    public UserDTO createUser(UserCreateRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("用户", "username", request.getUsername());
        }

        validatePasswordStrength(request.getPassword());

        User user = new User();
        BeanUtils.copyProperties(request, user);

        // 加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user = userRepository.save(user);

        return toDTO(user);
    }

    /**
     * 更新用户
     * 更新时清除缓存
     */
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "users", key = "#userId"),
        @CacheEvict(value = "users", allEntries = true)
    })
    public UserDTO updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDepartmentId() != null) {
            user.setDepartmentId(request.getDepartmentId());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        user = userRepository.save(user);

        return toDTO(user);
    }

    /**
     * 删除用户（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        user.setDeleted(true);
        userRepository.save(user);
    }

    /**
     * 获取用户列表
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> getUserList(int page, int size, String keyword, Long departmentId, Integer status) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "id"));

        // 这里可以添加更多查询条件
        Page<User> userPage = userRepository.findAll(pageable);

        return userPage.map(this::toDTO);
    }

    /**
     * 获取用户详情
     * 使用缓存，key为用户ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId")
    public UserDTO getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        return toDTO(user);
    }

    /**
     * 启用/禁用用户
     * 更新状态时清除缓存
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", key = "#userId")
    public void toggleUserStatus(Long userId, Integer status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        UserStatus userStatus = UserStatus.fromCode(status);
        user.setStatus(userStatus.getCode());

        log.info("用户状态变更: userId={}, username={}, status={}",
            userId, user.getUsername(), userStatus.getDescription());

        userRepository.save(user);
    }

    /**
     * 重置密码
     * 重置密码时清除缓存（安全考虑）
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", key = "#userId")
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new InvalidParameterException("newPassword", "新密码不能为空");
        }

        // 验证密码强度
        validatePasswordStrength(newPassword);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 修改密码
     * 修改密码时清除缓存（安全考虑）
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", key = "#userId")
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new InvalidParameterException("oldPassword", "旧密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new InvalidParameterException("newPassword", "新密码不能为空");
        }

        // 验证密码强度
        validatePasswordStrength(newPassword);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidParameterException("oldPassword", "原密码不正确");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 验证密码强度
     * 要求：至少8位，包含大小写字母和数字
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new InvalidParameterException("password", "密码长度不能少于8位");
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        if (!hasUppercase || !hasLowercase || !hasDigit) {
            throw new InvalidParameterException("password",
                "密码必须包含大写字母、小写字母和数字");
        }
    }

    /**
     * 分配角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户", userId));

        // 删除现有角色
        userRoleRepository.deleteByUserId(userId);

        // 分配新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> userRoles = roleIds.stream()
                    .map(roleId -> {
                        UserRole userRole = new UserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .collect(Collectors.toList());

            userRoleRepository.saveAll(userRoles);
        }
    }

    /**
     * 更新最后登录时间
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginTime(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    // 辅助方法

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);

        UserStatus userStatus = UserStatus.fromCode(user.getStatus());
        dto.setStatusDesc(userStatus.getDescription());

        // 设置部门名称
        if (user.getDepartmentId() != null) {
            departmentRepository.findById(user.getDepartmentId())
                    .ifPresent(dept -> dto.setDepartmentName(dept.getDeptName()));
        }

        // 设置角色列表
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        List<String> roles = userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId())
                        .map(role -> role.getRoleName())
                        .orElse("未知角色"))
                .collect(Collectors.toList());
        dto.setRoles(roles);

        return dto;
    }
}
