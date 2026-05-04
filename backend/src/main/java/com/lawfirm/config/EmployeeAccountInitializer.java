package com.lawfirm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.entity.*;
import com.lawfirm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 员工账号批量初始化器
 * 从employees_data.json读取员工信息并创建账号
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_NAME = "曾进朗";

    @Override
    public void run(String... args) {
        try {
            // 检查是否已初始化过员工账号
            if (userRepository.count() > 1) {
                log.info("员工账号已初始化，跳过");
                return;
            }

            log.info("开始批量创建员工账号...");

            // 读取员工数据
            List<Map<String, Object>> employees = loadEmployeeData();
            if (employees.isEmpty()) {
                log.warn("未找到员工数据文件");
                return;
            }

            // 获取角色映射
            Map<String, Role> roleMap = getRoleMapping();
            if (roleMap.isEmpty()) {
                log.error("角色未初始化，请先运行DataInitializer");
                return;
            }

            // 获取部门
            Department adminDept = departmentRepository.findAll().stream()
                    .filter(d -> "行政部".equals(d.getDeptName()))
                    .findFirst()
                    .orElse(null);

            int successCount = 0;
            int skipCount = 0;

            for (Map<String, Object> empData : employees) {
                try {
                    String name = (String) empData.get("姓名");
                    String idCard = (String) empData.get("身份证号码");
                    String type = (String) empData.get("类型");
                    String phone = empData.get("手机号码") != null ?
                            String.valueOf(empData.get("手机号码")) : null;

                    if (name == null || name.trim().isEmpty()) {
                        skipCount++;
                        continue;
                    }

                    // 检查用户是否已存在
                    if (userRepository.existsByUsername(name)) {
                        log.debug("用户已存在，跳过: {}", name);
                        skipCount++;
                        continue;
                    }

                    // 提取身份证后4位作为密码
                    String password = extractLast4Digits(idCard);

                    // 确定角色
                    Role role = determineRole(name, type, roleMap);

                    // 创建用户
                    User user = new User();
                    user.setUsername(name);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setRealName(name);
                    user.setPhone(phone);
                    user.setPosition(type);
                    user.setDepartmentId(adminDept != null ? adminDept.getId() : null);
                    user.setStatus(1);
                    user.setDeleted(false);

                    User savedUser = userRepository.save(user);

                    // 分配角色
                    UserRole userRole = new UserRole();
                    userRole.setUserId(savedUser.getId());
                    userRole.setRoleId(role.getId());
                    userRoleRepository.save(userRole);

                    successCount++;
                    log.info("创建用户成功: {} ({})", name, role.getRoleCode());

                } catch (Exception e) {
                    log.error("创建用户失败: {}", empData.get("姓名"), e);
                }
            }

            log.info("员工账号初始化完成！成功: {}, 跳过: {}", successCount, skipCount);

        } catch (Exception e) {
            log.error("员工账号初始化失败", e);
        }
    }

    /**
     * 从classpath加载employees_data.json
     */
    private List<Map<String, Object>> loadEmployeeData() {
        try {
            // 优先从项目根目录读取
            java.io.File file = new java.io.File("employees_data.json");
            if (file.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(file,
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            }

            // 从classpath读取
            InputStream is = getClass().getClassLoader().getResourceAsStream("employees_data.json");
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(is,
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            }

            log.warn("未找到employees_data.json文件");
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("读取员工数据失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取角色映射
     */
    private Map<String, Role> getRoleMapping() {
        Map<String, Role> roleMap = new HashMap<>();

        // ADMIN - 管理员
        roleRepository.findByRoleCode("ADMIN").ifPresent(r -> roleMap.put("ADMIN", r));

        // DIRECTOR - 主任/管理层
        roleRepository.findByRoleCode("DIRECTOR").ifPresent(r -> roleMap.put("DIRECTOR", r));

        // LAWYER_MAIN - 主办律师
        roleRepository.findByRoleCode("LAWYER_MAIN").ifPresent(r -> roleMap.put("LAWYER_MAIN", r));

        // ASSISTANT - 律师助理
        roleRepository.findByRoleCode("ASSISTANT").ifPresent(r -> roleMap.put("ASSISTANT", r));

        // FINANCE - 财务/行政
        roleRepository.findByRoleCode("FINANCE").ifPresent(r -> roleMap.put("FINANCE", r));

        return roleMap;
    }

    /**
     * 确定用户角色
     */
    private Role determineRole(String name, String type, Map<String, Role> roleMap) {
        // 曾进朗是管理员
        if (ADMIN_NAME.equals(name)) {
            return roleMap.get("ADMIN");
        }

        // 根据类型分配角色
        if ("执业律师".equals(type)) {
            return roleMap.get("LAWYER_MAIN");
        } else if ("实习律师".equals(type) || "律师助理".equals(type)) {
            return roleMap.get("ASSISTANT");
        } else if ("行政人员".equals(type) || type.contains("行政")) {
            return roleMap.get("FINANCE");
        }

        // 默认为律师助理
        return roleMap.get("ASSISTANT");
    }

    /**
     * 提取身份证后4位
     */
    private String extractLast4Digits(String idCard) {
        if (idCard == null || idCard.length() < 4) {
            return "1234"; // 默认密码
        }
        return idCard.substring(idCard.length() - 4);
    }
}
