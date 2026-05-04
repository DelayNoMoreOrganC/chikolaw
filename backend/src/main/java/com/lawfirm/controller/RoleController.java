package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.RoleCreateRequest;
import com.lawfirm.dto.RoleDTO;
import com.lawfirm.service.RoleService;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器
 */
@Slf4j
@RestController
@RequestMapping("roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 创建角色
     * POST /api/role
     */
    @PostMapping
    @AuditLog(value = "创建角色", operationType = "CREATE")
    public Result<RoleDTO> createRole(@Valid @RequestBody RoleCreateRequest request) {
        try {
            RoleDTO result = roleService.createRole(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建角色失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新角色
     * PUT /api/role/{id}
     */
    @PutMapping("/{id}")
    @AuditLog(value = "更新角色", operationType = "UPDATE")
    public Result<RoleDTO> updateRole(@PathVariable Long id,
                                     @Valid @RequestBody RoleCreateRequest request) {
        try {
            RoleDTO result = roleService.updateRole(id, request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新角色失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除角色
     * DELETE /api/role/{id}
     */
    @DeleteMapping("/{id}")
    @AuditLog(value = "删除角色", operationType = "DELETE")
    public Result<Void> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除角色失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取角色列表
     * GET /api/role
     */
    @GetMapping
    public Result<Page<RoleDTO>> getRoleList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<RoleDTO> result = roleService.getRoleList(page, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取角色列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取角色详情
     * GET /api/role/{id}
     */
    @GetMapping("/{id}")
    public Result<RoleDTO> getRoleDetail(@PathVariable Long id) {
        try {
            RoleDTO result = roleService.getRoleDetail(id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取角色详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分配权限
     * PUT /api/role/{id}/permissions
     */
    @PutMapping("/{id}/permissions")
    @AuditLog(value = "分配角色权限", operationType = "UPDATE")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                         @RequestBody Map<String, List<Long>> params) {
        try {
            List<Long> permissionIds = params.get("permissionIds");
            roleService.assignPermissions(id, permissionIds);
            return Result.success();
        } catch (Exception e) {
            log.error("分配权限失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有角色（下拉选择用）
     * GET /api/role/all
     */
    @GetMapping("/all")
    public Result<List<RoleDTO>> getAllRoles() {
        try {
            List<RoleDTO> result = roleService.getAllRoles();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取所有角色失败", e);
            return Result.error(e.getMessage());
        }
    }
}
