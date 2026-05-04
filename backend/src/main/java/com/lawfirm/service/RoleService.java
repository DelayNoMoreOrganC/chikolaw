package com.lawfirm.service;

import com.lawfirm.dto.RoleCreateRequest;
import com.lawfirm.dto.RoleDTO;
import com.lawfirm.entity.Role;
import com.lawfirm.entity.RolePermission;
import com.lawfirm.repository.PermissionRepository;
import com.lawfirm.repository.RolePermissionRepository;
import com.lawfirm.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务
 * 支持缓存：角色和权限查询结果会被缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * 创建角色
     * 创建时清除roles缓存
     */
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public RoleDTO createRole(RoleCreateRequest request) {
        // 检查角色编码是否已存在
        if (roleRepository.existsByRoleCode(request.getRoleCode())) {
            throw new RuntimeException("角色编码已存在");
        }

        Role role = new Role();
        BeanUtils.copyProperties(request, role);

        role = roleRepository.save(role);

        // 分配权限
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }

        return toDTO(role);
    }

    /**
     * 更新角色
     * 更新时清除缓存
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "roles", key = "#roleId"),
        @CacheEvict(value = "roles", allEntries = true)
    })
    public RoleDTO updateRole(Long roleId, RoleCreateRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());

        role = roleRepository.save(role);

        // 更新权限
        if (request.getPermissionIds() != null) {
            assignPermissions(roleId, request.getPermissionIds());
        }

        return toDTO(role);
    }

    /**
     * 删除角色
     * 删除时清除缓存
     */
    @Transactional
    @CacheEvict(value = "roles", key = "#roleId")
    public void deleteRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        // 删除角色权限关联
        rolePermissionRepository.deleteByRoleId(roleId);

        // 逻辑删除角色
        role.setDeleted(true);
        roleRepository.save(role);
    }

    /**
     * 获取角色列表
     */
    public Page<RoleDTO> getRoleList(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Role> rolePage = roleRepository.findAll(pageable);

        return rolePage.map(this::toDTO);
    }

    /**
     * 获取角色详情
     * 使用缓存
     */
    @Cacheable(value = "roles", key = "#roleId")
    public RoleDTO getRoleDetail(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        return toDTO(role);
    }

    /**
     * 分配权限
     * 分配权限时清除角色缓存
     */
    @Transactional
    @CacheEvict(value = "roles", key = "#roleId")
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        // 删除现有权限
        rolePermissionRepository.deleteByRoleId(roleId);

        // 分配新权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> rolePermissions = permissionIds.stream()
                    .map(permissionId -> {
                        RolePermission rolePermission = new RolePermission();
                        rolePermission.setRoleId(roleId);
                        rolePermission.setPermissionId(permissionId);
                        return rolePermission;
                    })
                    .collect(Collectors.toList());

            rolePermissionRepository.saveAll(rolePermissions);
        }
    }

    /**
     * 获取所有角色（下拉选择用）
     * 使用缓存
     */
    @Cacheable(value = "roles", key = "'all'")
    public List<RoleDTO> getAllRoles() {
        List<Role> roles = roleRepository.findAll();

        return roles.stream()
                .map(this::toSimpleDTO)
                .collect(Collectors.toList());
    }

    // 辅助方法

    private RoleDTO toDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        BeanUtils.copyProperties(role, dto);

        // 获取权限列表
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(role.getId());
        List<String> permissions = rolePermissions.stream()
                .map(rp -> permissionRepository.findById(rp.getPermissionId())
                        .map(permission -> permission.getPermissionCode())
                        .orElse("未知权限"))
                .collect(Collectors.toList());
        dto.setPermissions(permissions);

        return dto;
    }

    private RoleDTO toSimpleDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        BeanUtils.copyProperties(role, dto);
        return dto;
    }
}
