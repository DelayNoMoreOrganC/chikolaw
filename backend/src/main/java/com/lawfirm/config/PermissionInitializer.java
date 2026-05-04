package com.lawfirm.config;

import com.lawfirm.entity.Permission;
import com.lawfirm.entity.Role;
import com.lawfirm.entity.RolePermission;
import com.lawfirm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 权限初始化器
 * 创建系统权限并为各角色分配默认权限
 */
@Slf4j
@Component
@Order(2) // 在DataInitializer之后执行
@RequiredArgsConstructor
public class PermissionInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public void run(String... args) {
        try {
            if (permissionRepository.count() > 0) {
                log.info("权限已初始化，跳过");
                return;
            }

            log.info("开始初始化系统权限...");

            // 创建权限树
            Map<String, Permission> permissionMap = createPermissions();

            // 为角色分配权限
            assignRolePermissions(permissionMap);

            log.info("权限初始化完成！");

        } catch (Exception e) {
            log.error("权限初始化失败", e);
        }
    }

    /**
     * 创建系统权限
     */
    private Map<String, Permission> createPermissions() {
        Map<String, Permission> permissionMap = new HashMap<>();

        // 一级菜单
        Permission dashboard = createPermission("dashboard", "工作台", "MENU", 0, 1, null);
        Permission caseMgmt = createPermission("case", "案件管理", "MENU", 0, 2, null);
        Permission docMgmt = createPermission("document", "文档管理", "MENU", 0, 3, null);
        Permission calendar = createPermission("calendar", "日程管理", "MENU", 0, 4, null);
        Permission audit = createPermission("audit", "审计日志", "MENU", 0, 5, null);
        Permission sysMgmt = createPermission("system", "系统管理", "MENU", 0, 6, null);

        // 案件管理子菜单
        Permission caseList = createPermission("case:list", "案件列表", "MENU", 1, 1, "case");
        Permission caseCreate = createPermission("case:create", "创建案件", "BUTTON", 1, 2, "case");
        Permission caseEdit = createPermission("case:edit", "编辑案件", "BUTTON", 1, 3, "case");
        Permission caseDelete = createPermission("case:delete", "删除案件", "BUTTON", 1, 4, "case");
        Permission caseView = createPermission("case:view", "查看案件", "BUTTON", 1, 5, "case");

        // 文档管理子菜单
        Permission docList = createPermission("document:list", "文档列表", "MENU", 2, 1, "document");
        Permission docUpload = createPermission("document:upload", "上传文档", "BUTTON", 2, 2, "document");
        Permission docDownload = createPermission("document:download", "下载文档", "BUTTON", 2, 3, "document");
        Permission docDelete = createPermission("document:delete", "删除文档", "BUTTON", 2, 4, "document");

        // 日程管理子菜单
        Permission calendarList = createPermission("calendar:list", "日程列表", "MENU", 3, 1, "calendar");
        Permission calendarCreate = createPermission("calendar:create", "创建日程", "BUTTON", 3, 2, "calendar");
        Permission calendarEdit = createPermission("calendar:edit", "编辑日程", "BUTTON", 3, 3, "calendar");
        Permission calendarDelete = createPermission("calendar:delete", "删除日程", "BUTTON", 3, 4, "calendar");

        // 系统管理子菜单
        Permission userMgmt = createPermission("system:user", "用户管理", "MENU", 4, 1, "system");
        Permission roleMgmt = createPermission("system:role", "角色管理", "MENU", 4, 2, "system");
        Permission deptMgmt = createPermission("system:dept", "部门管理", "MENU", 4, 3, "system");
        Permission configMgmt = createPermission("system:config", "系统配置", "MENU", 4, 4, "system");

        // 保存所有权限
        List<Permission> permissions = Arrays.asList(
                dashboard, caseMgmt, docMgmt, calendar, audit, sysMgmt,
                caseList, caseCreate, caseEdit, caseDelete, caseView,
                docList, docUpload, docDownload, docDelete,
                calendarList, calendarCreate, calendarEdit, calendarDelete,
                userMgmt, roleMgmt, deptMgmt, configMgmt
        );

        List<Permission> saved = permissionRepository.saveAll(permissions);

        // 构建权限映射
        for (Permission p : saved) {
            permissionMap.put(p.getPermissionCode(), p);
        }

        return permissionMap;
    }

    /**
     * 为角色分配权限
     */
    private void assignRolePermissions(Map<String, Permission> permissionMap) {
        // 获取所有角色
        Optional<Role> adminOpt = roleRepository.findByRoleCode("ADMIN");
        Optional<Role> directorOpt = roleRepository.findByRoleCode("DIRECTOR");
        Optional<Role> lawyerMainOpt = roleRepository.findByRoleCode("LAWYER_MAIN");
        Optional<Role> assistantOpt = roleRepository.findByRoleCode("ASSISTANT");
        Optional<Role> financeOpt = roleRepository.findByRoleCode("FINANCE");

        // ADMIN - 全部权限
        if (adminOpt.isPresent()) {
            grantAllPermissions(adminOpt.get(), permissionMap);
            log.info("为ADMIN角色分配全部权限");
        }

        // DIRECTOR - 管理层：全部权限（除了用户管理）
        if (directorOpt.isPresent()) {
            List<String> perms = Arrays.asList(
                    "dashboard", "case", "document", "calendar", "audit",
                    "case:list", "case:create", "case:edit", "case:view",
                    "document:list", "document:upload", "document:download",
                    "calendar:list", "calendar:create", "calendar:edit", "calendar:delete",
                    "system:dept", "system:config"
            );
            grantPermissions(directorOpt.get(), perms, permissionMap);
            log.info("为DIRECTOR角色分配管理层权限");
        }

        // LAWYER_MAIN - 律师：案件、文档、日程
        if (lawyerMainOpt.isPresent()) {
            List<String> perms = Arrays.asList(
                    "dashboard",
                    "case", "case:list", "case:create", "case:edit", "case:view",
                    "document", "document:list", "document:upload", "document:download",
                    "calendar", "calendar:list", "calendar:create", "calendar:edit", "calendar:delete"
            );
            grantPermissions(lawyerMainOpt.get(), perms, permissionMap);
            log.info("为LAWYER_MAIN角色分配律师权限");
        }

        // ASSISTANT - 律师助理：案件查看、文档、日程
        if (assistantOpt.isPresent()) {
            List<String> perms = Arrays.asList(
                    "dashboard",
                    "case", "case:list", "case:view",
                    "document", "document:list", "document:upload", "document:download",
                    "calendar", "calendar:list", "calendar:create", "calendar:edit", "calendar:delete"
            );
            grantPermissions(assistantOpt.get(), perms, permissionMap);
            log.info("为ASSISTANT角色分配律师助理权限");
        }

        // FINANCE - 行政助理：基础权限
        if (financeOpt.isPresent()) {
            List<String> perms = Arrays.asList(
                    "dashboard",
                    "case", "case:list", "case:view",
                    "document", "document:list", "document:upload", "document:download"
            );
            grantPermissions(financeOpt.get(), perms, permissionMap);
            log.info("为FINANCE角色分配行政助理权限");
        }
    }

    private Permission createPermission(String code, String name, String type, int parentId, int sort, String parentCode) {
        Permission p = new Permission();
        p.setPermissionCode(code);
        p.setPermissionName(name);
        p.setResourceType(type);
        p.setParentId((long) parentId);
        p.setSortOrder(sort);
        return p;
    }

    private void grantAllPermissions(Role role, Map<String, Permission> permissionMap) {
        List<RolePermission> rolePermissions = new ArrayList<>();
        for (Permission p : permissionMap.values()) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(role.getId());
            rp.setPermissionId(p.getId());
            rolePermissions.add(rp);
        }
        rolePermissionRepository.saveAll(rolePermissions);
    }

    private void grantPermissions(Role role, List<String> permCodes, Map<String, Permission> permissionMap) {
        List<RolePermission> rolePermissions = new ArrayList<>();
        for (String code : permCodes) {
            Permission p = permissionMap.get(code);
            if (p != null) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(p.getId());
                rolePermissions.add(rp);
            }
        }
        rolePermissionRepository.saveAll(rolePermissions);
    }
}
